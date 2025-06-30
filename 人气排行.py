import requests
from lxml import etree
import csv
import time
import random
from fake_useragent import UserAgent
import logging
from bs4 import BeautifulSoup
import os
from tqdm import tqdm
from urllib.parse import urljoin

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# 车型分类配置
CAR_CATEGORIES = {
    1: "微型车",
    2: "小型车",
    3: "紧凑型车",
    4: "中型车",
    5: "中大型车",
    6: "MPV",
    7: "SUV",
    8: "大型车",
    9: "跑车",
    10: "轻客"
}

BASE_URL = "https://top.16888.com/auto_rqjb{type}_{page}.html"
OUTPUT_DIR = "all_car_rankings"  # 总数据存储目录
IMAGE_DIR = os.path.join(OUTPUT_DIR, "images")  # 图片存储目录
MAX_RETRIES = 3  # 请求最大重试次数
REQUEST_DELAY = (3, 7)  # 请求间隔范围(秒)

def get_random_headers():
    """生成随机请求头，增强反爬绕过能力"""
    ua = UserAgent()
    headers = {
        'User-Agent': ua.random,
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
        'Accept-Encoding': 'gzip, deflate, br',
        'Connection': 'keep-alive',
        'Upgrade-Insecure-Requests': '1',
        'Cache-Control': 'max-age=0',
        'Referer': 'https://www.16888.com/'
    }
    return headers

def get_page_content(url, max_retries=MAX_RETRIES):
    """带重试机制的页面获取函数"""
    for attempt in range(max_retries):
        try:
            headers = get_random_headers()
            logger.info(f"请求URL: {url}")
            response = requests.get(url, headers=headers, timeout=15)
            
            if response.status_code == 200:
                if 'text/html' in response.headers.get('Content-Type', ''):
                    response.encoding = 'utf-8'
                    logger.info(f"成功获取页面: {url}")
                    return response.text
                else:
                    logger.warning(f"非HTML响应: {url}")
                    return None
            else:
                logger.warning(f"请求失败，状态码: {response.status_code}, URL: {url}")
                if response.status_code in [429, 503]:
                    wait_time = random.uniform(5, 10)
                    logger.info(f"等待{wait_time:.2f}秒后重试...")
                    time.sleep(wait_time)
                else:
                    break
        except Exception as e:
            logger.error(f"请求异常: {e}, URL: {url}")
            if attempt < max_retries - 1:
                wait_time = random.uniform(2, 5)
                logger.info(f"等待{wait_time:.2f}秒后重试...")
                time.sleep(wait_time)
    logger.error(f"放弃请求: {url}")
    return None

def download_image(url, save_path, max_retries=MAX_RETRIES):
    """下载图片并保存到指定路径"""
    if not url:
        logger.warning("图片URL为空，跳过下载")
        return False
    
    for attempt in range(max_retries):
        try:
            headers = get_random_headers()
            logger.info(f"下载图片: {url}")
            
            # 添加Referer以绕过图片防盗链
            if '16888.com' in url:
                headers['Referer'] = 'https://www.16888.com/'
                
            response = requests.get(url, headers=headers, timeout=30, stream=True)
            
            if response.status_code == 200:
                # 确保目录存在
                os.makedirs(os.path.dirname(save_path), exist_ok=True)
                
                with open(save_path, 'wb') as f:
                    for chunk in response.iter_content(1024):
                        f.write(chunk)
                
                logger.info(f"图片保存成功: {save_path}")
                return True
            else:
                logger.warning(f"下载失败，状态码: {response.status_code}, URL: {url}")
        except Exception as e:
            logger.error(f"下载异常: {e}, URL: {url}")
            if attempt < max_retries - 1:
                wait_time = random.uniform(2, 5)
                logger.info(f"等待{wait_time:.2f}秒后重试...")
                time.sleep(wait_time)
    
    logger.error(f"放弃下载图片: {url}")
    return False

def parse_page(html_content, category_name):
    """解析页面内容并提取数据"""
    if not html_content:
        return None, None
    
    try:
        soup = BeautifulSoup(html_content, 'html.parser')
        table = soup.find('table', class_='table_list')
        
        if not table:
            tables = soup.find_all('table')
            if tables:
                logger.info(f"找到{len(tables)}个表格，尝试解析第一个")
                table = tables[0]
            else:
                logger.error("未找到表格元素")
                return None, None
        
        table_html = str(table)
        tree = etree.HTML(table_html)
        
        # 提取表头（排除最后一列"车型相关"）
        headers = [th.text.strip() for th in tree.xpath('//tr[1]/th') if th.text.strip()][:-1]
        if not headers:
            logger.error("未找到表头")
            return None, None
        
        # 添加车型类别列
        if '车型类别' not in headers:
            headers.insert(1, '车型类别')
        # 添加图片路径列
        if '图片路径' not in headers:
            headers.append('图片路径')
        
        # 提取数据行
        data = []
        rows = tree.xpath('//tr[position() > 1]')  # 跳过表头行
        
        for row in rows:
            try:
                item = {}
                
                # 提取排名
                rank = row.xpath('.//td[1]//em/text()')
                item[headers[0]] = rank[0].strip() if rank else "未知"
                
                # 车型类别
                item[headers[1]] = category_name
                
                # 提取车型
                model = row.xpath('.//td[2]//a/text()')
                item[headers[2]] = model[0].strip() if model else "未知"
                
                # 提取车型详情页URL
                model_url = row.xpath('.//td[2]//a/@href')
                model_url = model_url[0] if model_url else ""
                
                # 提取指导价
                price = row.xpath('.//td[3]//a/text()')
                item[headers[3]] = price[0].strip() if price else "未知"
                
                # 提取关注度
                attention = row.xpath('.//td[4]/text()')
                item[headers[4]] = attention[0].strip() if attention else "未知"
                
                # 提取网友口碑
                reputation = row.xpath('.//td[5]//a/text()')
                item[headers[5]] = reputation[0].strip() if reputation else "未知"
                
                # 提取网友油耗
                fuel_consumption = row.xpath('.//td[6]//a/text()')
                item[headers[6]] = fuel_consumption[0].strip() if fuel_consumption else "未知"
                
                # 初始化图片路径
                item[headers[7]] = ""
                
                # 如果有车型详情页URL，则尝试获取图片
                if model_url:
                    # 构建完整URL
                    if not model_url.startswith('http'):
                        base_site = "https://www.16888.com/"
                        model_url = urljoin(base_site, model_url)
                    
                    # 随机延时后请求车型详情页
                    wait_time = random.uniform(*REQUEST_DELAY)
                    time.sleep(wait_time)
                    
                    model_page = get_page_content(model_url)
                    if model_page:
                        try:
                            model_tree = etree.HTML(model_page)
                            # 使用提供的XPath获取图片URL
                            img_elements = model_tree.xpath('/html/body/div[8]/div[2]/div[1]/div[1]/div[1]/span[1]/a/img')
                            
                            if img_elements:
                                img_url = img_elements[0].get('src', '')
                                
                                if img_url:
                                    # 构建完整图片URL
                                    if not img_url.startswith('http'):
                                        img_url = urljoin(base_site, img_url)
                                    
                                    # 构建图片保存路径
                                    model_name = item[headers[2]].replace('/', '_').replace('\\', '_')  # 处理非法文件名
                                    category_dir = os.path.join(IMAGE_DIR, category_name)
                                    img_filename = f"{model_name}_{random.randint(1000, 9999)}.jpg"
                                    img_path = os.path.join(category_dir, img_filename)
                                    
                                    # 下载图片
                                    if download_image(img_url, img_path):
                                        # 保存相对路径
                                        item[headers[7]] = os.path.relpath(img_path, OUTPUT_DIR)
                                    else:
                                        logger.warning(f"图片下载失败，车型: {model_name}")
                            else:
                                logger.warning(f"未找到车型图片，URL: {model_url}")
                        except Exception as e:
                            logger.error(f"解析车型详情页时出错: {e}, URL: {model_url}")
                
                data.append(item)
                
            except Exception as e:
                logger.error(f"解析行数据时出错: {e}")
        
        logger.info(f"成功解析{len(data)}条数据，车型: {category_name}")
        return headers, data
    
    except Exception as e:
        logger.error(f"解析页面时发生异常: {e}")
        return None, None

def save_data(headers, data, category_name):
    """保存数据到CSV文件"""
    if not headers or not data:
        logger.warning(f"没有可保存的数据，车型: {category_name}")
        return
    
    filename = os.path.join(OUTPUT_DIR, f"{category_name}.csv")
    
    try:
        # 检查文件是否存在，不存在则写入表头
        file_exists = os.path.isfile(filename)
        
        with open(filename, 'a', newline='', encoding='utf-8-sig') as f:
            writer = csv.DictWriter(f, fieldnames=headers)
            if not file_exists:
                writer.writeheader()
            writer.writerows(data)
        
        logger.info(f"成功保存{len(data)}条数据到: {filename}")
    except Exception as e:
        logger.error(f"保存数据时出错: {e}, 车型: {category_name}")

def crawl_category_pages(category_id, category_name):
    """爬取指定车型类别的所有页面（自动识别空页）"""
    logger.info(f"开始爬取车型类别: {category_name} (ID: {category_id})")
    
    page = 1
    all_headers = None
    total_data = 0
    consecutive_empty_pages = 0  # 连续空页数
    
    # 获取该类别总页数（用于进度条）
    first_page = get_page_content(BASE_URL.format(type=category_id, page=1))
    if not first_page:
        logger.error(f"无法获取第一页，跳过车型类别: {category_name}")
        return 0
    
    # 这里简化处理，假设最多100页（实际会根据空页检测提前终止）
    estimated_pages = 100
    
    with tqdm(total=estimated_pages, desc=f"爬取{category_name}") as pbar:
        while True:
            url = BASE_URL.format(type=category_id, page=page)
            
            # 增加请求间隔，避免频繁请求
            wait_time = random.uniform(*REQUEST_DELAY)
            time.sleep(wait_time)
            
            html_content = get_page_content(url)
            if not html_content:
                logger.warning(f"获取页面失败，页码: {page}, 车型: {category_name}")
                consecutive_empty_pages += 1
                if consecutive_empty_pages >= 2:  # 连续2次获取失败则停止
                    break
                page += 1
                pbar.update(1)
                continue
            
            headers, data = parse_page(html_content, category_name)
            
            # 处理解析结果
            if not headers or not data:
                logger.warning(f"解析页面失败或无数据，页码: {page}, 车型: {category_name}")
                consecutive_empty_pages += 1
                if consecutive_empty_pages >= 2:  # 连续2次解析无数据则停止
                    break
            else:
                consecutive_empty_pages = 0  # 重置连续空页计数
                if not all_headers:
                    all_headers = headers
                save_data(all_headers, data, category_name)
                total_data += len(data)
            
            page += 1
            pbar.update(1)
            
            # 更新进度条总页数（当检测到空页时）
            if not data and page > 1:
                pbar.total = page - 1
                pbar.refresh()
                logger.info(f"检测到空页，停止爬取，车型: {category_name}，实际总页数: {page-1}")
                break
    
    logger.info(f"完成爬取车型类别: {category_name}, 共获取{total_data}条数据")
    return total_data

def main():
    """主函数，控制多车型爬取流程"""
    logger.info("开始执行增强版爬虫程序（带图片爬取和进度条）")
    
    total_cars = 0
    failed_categories = []
    
    # 确保总目录和图片目录存在
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    os.makedirs(IMAGE_DIR, exist_ok=True)
    logger.info(f"已创建数据目录: {OUTPUT_DIR}")
    logger.info(f"已创建图片目录: {IMAGE_DIR}")
    
    # 计算总任务数（用于总体进度条）
    total_tasks = len(CAR_CATEGORIES)
    
    with tqdm(total=total_tasks, desc="总体进度") as overall_pbar:
        for category_id, category_name in CAR_CATEGORIES.items():
            try:
                category_cars = crawl_category_pages(category_id, category_name)
                total_cars += category_cars
                overall_pbar.set_postfix({category_name: f"{category_cars}条"})
            except Exception as e:
                logger.error(f"爬取车型类别时发生异常: {e}, 车型: {category_name}")
                failed_categories.append(category_name)
            overall_pbar.update(1)
    
    logger.info(f"爬虫执行完成")
    logger.info(f"总共爬取{len(CAR_CATEGORIES)}个车型类别")
    logger.info(f"成功获取{total_cars}条车型数据")
    logger.info(f"数据已保存至: {OUTPUT_DIR}")
    logger.info(f"图片已保存至: {IMAGE_DIR}")
    
    if failed_categories:
        logger.warning(f"以下车型类别爬取失败: {', '.join(failed_categories)}")
        logger.warning("建议单独爬取这些类别或检查网络连接")

if __name__ == "__main__":
    main()