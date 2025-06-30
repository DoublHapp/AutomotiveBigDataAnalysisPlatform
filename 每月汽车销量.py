import requests
from lxml import html
import pandas as pd
import time
import os
import random
from datetime import datetime, timedelta
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urlparse
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    filename='crawler.log'
)
logger = logging.getLogger(__name__)

# 基础配置
base_url = 'https://xl.16888.com/style-{month}-{month}-{page}.html'

# 随机User-Agent池
USER_AGENTS = [
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
    'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36',
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:90.0) Gecko/20100101 Firefox/90.0',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 11.5; rv:90.0) Gecko/20100101 Firefox/90.0',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Mobile/15E148 Safari/604.1'
]

# 代理IP池（示例，实际使用时需替换为有效代理）
PROXY_POOL = [
    # {'http': 'http://user:pass@ip:port', 'https': 'http://user:pass@ip:port'},
    # 添加更多代理...
]

# 休眠时间设置（秒）
MIN_SLEEP = 1
MAX_SLEEP = 3

# 创建输出目录
if not os.path.exists('car_sales_data'):
    os.makedirs('car_sales_data')

# 获取随机User-Agent
def get_random_user_agent():
    return random.choice(USER_AGENTS)

# 获取随机代理
def get_random_proxy():
    if PROXY_POOL:
        return random.choice(PROXY_POOL)
    return None

# 获取请求头
def get_headers():
    return {
        'User-Agent': get_random_user_agent(),
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
        'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
        'Connection': 'keep-alive',
        'Referer': 'https://xl.16888.com/'
    }

# 获取用户输入的爬取时间段
def get_user_time_range():
    print("="*50)
    print("车主之家车型销量数据爬虫")
    print("="*50)
    
    while True:
        try:
            print("请输入爬取的时间段（格式：YYYY-MM）")
            start_date = input("开始年月（例如2016-01）：").strip()
            end_date = input("结束年月（例如2025-05）：").strip()
            
            start_year, start_month = map(int, start_date.split('-'))
            end_year, end_month = map(int, end_date.split('-'))
            
            # 验证日期范围
            if not (2016 <= start_year <= 2025 and 1 <= start_month <= 12):
                print("开始日期超出范围，请重新输入")
                continue
            
            if not (2016 <= end_year <= 2025 and 1 <= end_month <= 12):
                print("结束日期超出范围，请重新输入")
                continue
            
            # 检查结束日期是否晚于开始日期
            if (end_year < start_year) or (end_year == start_year and end_month < start_month):
                print("结束日期不能早于开始日期，请重新输入")
                continue
            
            return start_year, start_month, end_year, end_month
        except:
            print("格式错误，请使用YYYY-MM格式")

# 生成需要爬取的月份列表
def generate_month_range(start_year, start_month, end_year, end_month):
    months = []
    current = datetime(start_year, start_month, 1)
    end = datetime(end_year, end_month, 1)
    
    while current <= end:
        months.append(current.strftime('%Y%m'))
        current = current + timedelta(days=32)  # 确保跨月
        current = current.replace(day=1)  # 重置为每月第一天
    
    return months

# 获取指定月份的总页数
def get_total_pages(month):
    url = base_url.format(month=month, page=1)
    max_retries = 3
    
    for attempt in range(max_retries):
        try:
            proxy = get_random_proxy()
            response = requests.get(
                url, 
                headers=get_headers(), 
                proxies=proxy,
                timeout=15
            )
            response.raise_for_status()
            tree = html.fromstring(response.text)
            
            # 使用提供的XPath解析最后一页数字
            last_page_elements = tree.xpath('/html/body/div[5]/div[3]/div[2]/div/div[2]/div[2]/div[1]/div/a[6]/text()')
            
            if last_page_elements:
                return int(last_page_elements[0].strip())
            
            # 尝试其他可能的分页XPath
            other_pagination_xpaths = [
                "//div[contains(@class, 'pagination')]//a[last()-1]/text()",
                "//div[contains(@class, 'pages')]//a[last()-1]/text()"
            ]
            
            for xpath in other_pagination_xpaths:
                elements = tree.xpath(xpath)
                if elements:
                    return int(elements[0].strip())
            
            logger.warning(f"无法确定{month}的总页数，默认按1页处理")
            return 1
        
        except Exception as e:
            logger.error(f"获取{month}总页数尝试{attempt+1}/{max_retries}失败: {e}")
            if attempt == max_retries - 1:
                logger.error(f"获取{month}总页数达到最大重试次数")
                return 1
            # 指数退避
            time.sleep(2 ** attempt + random.random())

# 爬取单个页面的数据
def crawl_page(url, retry_count=0):
    max_retries = 3
    
    try:
        proxy = get_random_proxy()
        response = requests.get(
            url, 
            headers=get_headers(), 
            proxies=proxy,
            timeout=15
        )
        response.raise_for_status()
        
        # 检查是否被反爬
        if '验证码' in response.text or response.status_code == 429:
            raise Exception("触发反爬机制")
        
        tree = html.fromstring(response.text)
        
        # 尝试多种XPath定位表格
        table_xpaths = [
            "//table[contains(@class, 'xl-table')]",
            "/html/body/div[5]/div[3]/div[2]/div/div[2]/div[1]/table",
            "//table[position()=1]"
        ]
        
        table = None
        for xpath in table_xpaths:
            tables = tree.xpath(xpath)  # 获取元素列表
            if len(tables) > 0:  # 明确检查列表长度
                table = tables[0]
                break
        
        if table is None:  # 明确检查是否为None
            logger.warning(f"未找到表格: {url}")
            return []
        
        # 获取数据行（跳过表头）
        rows = table.xpath(".//tr[position()>1]")
        if len(rows) == 0:  # 明确检查列表长度
            logger.warning(f"未找到数据行: {url}")
            return []
        
        data = []
        for row in rows:
            try:
                rank = row.xpath(".//td[1]/text()")
                rank = rank[0].strip() if rank else "未知"
                
                model = row.xpath(".//td[2]//text()")
                model = next((text.strip() for text in model if text.strip()), "未知")
                
                sales = row.xpath(".//td[3]/text()")
                sales = sales[0].strip() if sales else "未知"
                
                manufacturer = row.xpath(".//td[4]//text()")
                manufacturer = next((text.strip() for text in manufacturer if text.strip()), "未知")
                
                price = row.xpath(".//td[5]//text()")
                price = next((text.strip() for text in price if text.strip()), "未知")
                
                data.append({
                    "排名": rank,
                    "车型": model,
                    "销量": sales,
                    "厂商": manufacturer,
                    "售价（万元）": price,
                    "月份": url.split('-')[1]
                })
            except Exception as e:
                logger.error(f"解析行时出错: {url}, 错误: {e}")
        
        # 随机休眠，避免请求过于频繁
        time.sleep(random.uniform(MIN_SLEEP, MAX_SLEEP))
        return data
    
    except Exception as e:
        logger.error(f"爬取页面失败: {url}, 错误: {e}")
        
        if retry_count < max_retries:
            # 指数退避重试
            wait_time = 2 ** retry_count + random.random()
            logger.info(f"将在{wait_time:.2f}秒后重试 ({retry_count+1}/{max_retries})")
            time.sleep(wait_time)
            return crawl_page(url, retry_count + 1)
        
        logger.error(f"达到最大重试次数: {url}")
        return []

# 爬取单个月份的数据（所有页）
def crawl_month(month):
    all_data = []
    
    # 获取该月总页数
    total_pages = get_total_pages(month)
    logger.info(f"{month} 总页数: {total_pages}")
    print(f"{month} 总页数: {total_pages}")
    
    # 并发爬取月份内的所有页面
    with ThreadPoolExecutor(max_workers=5) as executor:
        futures = []
        for page in range(1, total_pages + 1):
            url = base_url.format(month=month, page=page)
            futures.append(executor.submit(crawl_page, url))
        
        # 收集结果
        for future in futures:
            all_data.extend(future.result())
    
    logger.info(f"{month} 爬取完成，共 {len(all_data)} 条记录")
    print(f"{month} 爬取完成，共 {len(all_data)} 条记录")
    return all_data

# 主程序
def main():
    # 获取用户输入的时间段
    start_year, start_month, end_year, end_month = get_user_time_range()
    
    # 生成月份列表
    months = generate_month_range(start_year, start_month, end_year, end_month)
    print(f"将爬取 {len(months)} 个月份的数据: {months[0]} 至 {months[-1]}")
    
    total_records = 0
    user_confirm = input("确认开始爬取？(y/n): ").strip().lower()
    
    if user_confirm != 'y':
        print("操作已取消")
        return
    
    # 并发爬取多个月份
    with ThreadPoolExecutor(max_workers=3) as executor:
        futures = []
        for month in months:
            output_file = f"car_sales_data/{month}.csv"
            if os.path.exists(output_file):
                print(f"{month} 数据已存在，跳过")
                continue
            
            futures.append(executor.submit(crawl_month, month))
        
        # 保存结果
        for future in futures:
            month_data = future.result()
            if month_data:
                month = month_data[0]['月份']
                df = pd.DataFrame(month_data)
                df.to_csv(f"car_sales_data/{month}.csv", index=False, encoding="utf-8-sig")
                total_records += len(month_data)
    
    print(f"全部爬取完成！共爬取 {total_records} 条记录，保存到 car_sales_data 目录")
    logger.info(f"全部爬取完成！共爬取 {total_records} 条记录")

if __name__ == "__main__":
    main()    