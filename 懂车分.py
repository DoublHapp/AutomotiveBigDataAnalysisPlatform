import requests
from lxml import etree
import os
import json
import time
import random
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.parse import urljoin

# 配置信息
BASE_URL = "https://www.dongchedi.com"
MAX_RETRIES = 3  # 最大重试次数
DELAY_MIN = 1  # 最小延迟时间(秒)
DELAY_MAX = 3  # 最大延迟时间(秒)
MAX_WORKERS = 5  # 最大线程数
OUTPUT_DIR = "car_data"
IMAGE_DIR = os.path.join(OUTPUT_DIR, "car_images")
DATA_FILE = os.path.join(OUTPUT_DIR, "car_data.json")

# 随机User-Agent列表
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:90.0) Gecko/20100101 Firefox/90.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 11.5; rv:90.0) Gecko/20100101 Firefox/90.0"
]

# 创建必要的目录
os.makedirs(IMAGE_DIR, exist_ok=True)

def get_random_headers():
    """生成随机请求头"""
    return {
        "User-Agent": random.choice(USER_AGENTS),
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
        "Accept-Language": "zh-CN,zh;q=0.8,en;q=0.5,en-US;q=0.3",
        "Connection": "keep-alive",
        "Upgrade-Insecure-Requests": "1",
        "Cache-Control": "max-age=0"
    }

def fetch_url(url, retries=MAX_RETRIES):
    """发送HTTP请求并获取内容，带重试机制"""
    for attempt in range(retries):
        try:
            time.sleep(random.uniform(DELAY_MIN, DELAY_MAX))  # 随机延迟
            response = requests.get(url, headers=get_random_headers(), timeout=10)
            response.raise_for_status()
            return response.text
        except requests.RequestException as e:
            print(f"请求 {url} 失败 (尝试 {attempt+1}/{retries}): {e}")
            if attempt == retries - 1:
                print(f"已达到最大重试次数，跳过 {url}")
                return None

def parse_car_data(html_content, series_id):
    """解析汽车数据"""
    if not html_content:
        return None
    
    html = etree.HTML(html_content)
    
    # 解析汽车名
    car_name_element = html.xpath('/html/body/div[1]/div/div[2]/h1[2]/span/span[2]')
    car_name = car_name_element[0].text.strip() if car_name_element and car_name_element[0].text else ""
    if not car_name:
        print(f"未找到汽车名，跳过ID为 {series_id} 的页面")
        return None
    
    # 解析懂车分相关信息
    score = html.xpath('/html/body/div[1]/div/div[2]/div[2]/section[1]/div/div[1]/div[2]/span')
    rating_desc = html.xpath('/html/body/div[1]/div/div[2]/div[2]/section[1]/div/div[1]/div[2]/div[1]/div[2]')
    evaluator_count = html.xpath('/html/body/div[1]/div/div[2]/div[2]/section[1]/div/div[1]/div[2]/div[2]/div/span')
    dealer_price = html.xpath('/html/body/div[1]/div/div[2]/div[2]/section[1]/div/div[1]/div[4]/div[1]/span')
    mfrs_price = html.xpath('/html/body/div[1]/div/div[2]/div[2]/section[1]/div/div[1]/div[4]/div[2]/span')
    
    # 解析所有项目评分相关信息
    all_score_data = {}
    project_names = ["综合", "外观", "内饰", "配置", "空间", "舒适性", "操控", "动力"]
    for i in range(1, 9):
        items = html.xpath(f'/html/body/div[1]/div/div[2]/div[2]/section[1]/div/div[2]/div[2]/ul[{i}]/li[1]')
        current_scores = html.xpath(f'/html/body/div[1]/div/div[2]/div[2]/section[1]/div/div[2]/div[2]/ul[{i}]/li[2]')
        average_scores = html.xpath(f'/html/body/div[1]/div/div[2]/div[2]/section[1]/div/div[2]/div[2]/ul[{i}]/li[3]')
        if items and current_scores and average_scores:
            all_score_data[project_names[i - 1]] = {
                "current": current_scores[0].text.strip() if current_scores[0].text else "",
                "average": average_scores[0].text.strip() if average_scores[0].text else ""
            }
    
    # 解析汽车图片链接
    car_img_element = html.xpath('/html/body/div[1]/div/div[2]/div[2]/section[1]/div/img[1]')
    car_img_url = car_img_element[0].get('src') if car_img_element and car_img_element[0].get('src') else ""
    if car_img_url and not car_img_url.startswith('http'):
        car_img_url = urljoin(BASE_URL, car_img_url)
    
    return {
        "汽车名": car_name,
        "车系ID": series_id,
        "懂车分": score[0].text.strip() if score and score[0].text else "",
        "评级描述": rating_desc[0].text.strip() if rating_desc and rating_desc[0].text else "",
        "评价人数": evaluator_count[0].text.strip() if evaluator_count and evaluator_count[0].text else "",
        "经销商报价": dealer_price[0].text.strip() if dealer_price and dealer_price[0].text else "",
        "厂商指导价": mfrs_price[0].text.strip() if mfrs_price and mfrs_price[0].text else "",
        "各项目评分": all_score_data,
        "汽车图片链接": car_img_url
    }

def download_image(img_url, save_path, retries=2):
    """下载图片，带重试机制"""
    if not img_url:
        return False
    
    for attempt in range(retries):
        try:
            time.sleep(random.uniform(0.5, 1.5))  # 图片下载延迟
            response = requests.get(img_url, headers=get_random_headers(), timeout=10)
            response.raise_for_status()
            with open(save_path, 'wb') as f:
                f.write(response.content)
            return True
        except requests.RequestException as e:
            print(f"下载图片 {img_url} 失败 (尝试 {attempt+1}/{retries}): {e}")
    return False

def process_series(series_id):
    """处理单个车系"""
    url = f"{BASE_URL}/auto/series/score/{series_id}-x-x-x-x-x"
    html_content = fetch_url(url)
    car_data = parse_car_data(html_content, series_id)
    
    if car_data:
        # 下载图片
        if car_data["汽车图片链接"]:
            safe_car_name = "".join([c for c in car_data["汽车名"] if c.isalnum() or c in [' ', '_', '-']])
            img_path = os.path.join(IMAGE_DIR, f'{safe_car_name}_{series_id}.jpg')
            
            if download_image(car_data["汽车图片链接"], img_path):
                car_data["本地图片路径"] = img_path
                print(f"已下载图片: {img_path}")
            else:
                car_data["本地图片路径"] = None
                print(f"图片下载失败: {car_data['汽车图片链接']}")
        
        return car_data
    return None

def save_to_json(data, filename):
    """保存数据到JSON文件"""
    with open(filename, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=4)
    print(f"数据已保存到 {filename}")

def main():
    # 读取已爬取的ID
    crawled_ids = set()
    if os.path.exists(DATA_FILE):
        try:
            with open(DATA_FILE, 'r', encoding='utf-8') as f:
                existing_data = json.load(f)
                crawled_ids = {item.get("车系ID") for item in existing_data if item.get("车系ID")}
        except json.JSONDecodeError:
            print("JSON文件解析错误，将重新爬取所有数据")
    
    # 要爬取的车系ID范围
    series_ids = list(range(1, 6367))  # 示例范围，可根据需要修改
    series_ids_to_crawl = [sid for sid in series_ids if sid not in crawled_ids]
    
    if not series_ids_to_crawl:
        print("没有需要爬取的车系ID")
        return
    
    print(f"准备爬取 {len(series_ids_to_crawl)} 个车系数据")
    
    all_data = []
    # 如果已有数据，先加载
    if os.path.exists(DATA_FILE) and os.path.getsize(DATA_FILE) > 0:
        try:
            with open(DATA_FILE, 'r', encoding='utf-8') as f:
                all_data = json.load(f)
        except json.JSONDecodeError:
            print("无法加载现有数据，将创建新文件")
    
    # 使用线程池并发处理
    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        future_to_id = {executor.submit(process_series, sid): sid for sid in series_ids_to_crawl}
        
        for future in as_completed(future_to_id):
            series_id = future_to_id[future]
            try:
                car_data = future.result()
                if car_data:
                    all_data.append(car_data)
                    print(f"成功爬取车系ID {series_id}: {car_data['汽车名']}")
                    
                    # 每成功爬取10个车系就保存一次数据
                    if len(all_data) % 10 == 0:
                        save_to_json(all_data, DATA_FILE)
            except Exception as e:
                print(f"处理车系ID {series_id} 时发生错误: {e}")
    
    # 保存最终结果
    if all_data:
        save_to_json(all_data, DATA_FILE)
    else:
        print("未爬取到任何有效数据")

if __name__ == "__main__":
    main()    