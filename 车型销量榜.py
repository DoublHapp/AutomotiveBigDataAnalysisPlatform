import requests
from bs4 import BeautifulSoup
import pandas as pd
import time
import random
import os
from datetime import datetime

class YicheSalesSpider:
    def __init__(self):
        self.base_url = "https://car.yiche.com/newcar/salesrank/?cityId={}&saleType=2&{}&page={}"
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
            'Connection': 'keep-alive',
        }
        self.city_mapping = self.read_city_ids()
        self.time_params = self.read_time_params()
        self.max_retries = 5  # 最大重试次数
        self.retry_delay = 2  # 重试间隔(秒)
        self.output_file = "易车汽车销量数据.csv"  # 统一输出文件
        
        # 创建保存数据的目录
        if not os.path.exists('易车数据'):
            os.makedirs('易车数据')

    def read_city_ids(self):
        """读取城市.txt 文件，解析城市 ID 映射"""
        city_mapping = {}
        try:
            with open('易车/城市.txt', 'r', encoding='utf-8') as file:
                current_province = None
                for line in file:
                    line = line.strip()
                    if not line:
                        continue
                    if ':' in line:
                        city, city_id = line.split(':')
                        if '  ' in line:  # 子城市
                            if current_province:
                                city = f"{current_province}-{city}"
                        city_mapping[city_id] = city
                    else:
                        current_province = line
        except FileNotFoundError:
            print("未找到城市.txt 文件")
        return city_mapping

    def read_time_params(self):
        """读取时间.txt 文件，解析时间参数"""
        time_params = []
        try:
            with open('易车/时间.txt', 'r', encoding='utf-8') as file:
                for line in file:
                    line = line.strip()
                    if line:
                        time_params.append(line)
        except FileNotFoundError:
            print("未找到时间.txt 文件")
        return time_params

    def format_time(self, time_param):
        """格式化时间参数"""
        if time_param.startswith('flag='):
            return f"{time_param.split('=')[1]}年"
        elif time_param.startswith('date='):
            year, month, _ = time_param.split('=')[1].split('-')
            return f"{year}年{month}月"
        return "未知时间"

    def get_page(self, city_id, time_param, page_num):
        """获取指定页码的页面内容，带有重试机制"""
        city_name = self.city_mapping.get(city_id, city_id)
        formatted_time = self.format_time(time_param)
        
        for attempt in range(self.max_retries):
            try:
                url = self.base_url.format(city_id, time_param, page_num)
                response = requests.get(url, headers=self.headers, timeout=10)
                response.raise_for_status()  # 检查请求是否成功
                response.encoding = response.apparent_encoding
                
                # 验证页面内容是否有效
                if '很抱歉，没有找到相关内容' in response.text:
                    print(f"警告: {city_name}在{formatted_time}第{page_num}页没有找到相关内容")
                    return None
                    
                return response.text

            except requests.RequestException as e:
                print(f"尝试 {attempt+1}/{self.max_retries}: 请求 {city_name} 在 {formatted_time} 第 {page_num} 页时出错: {e}")
                if attempt < self.max_retries - 1:
                    wait_time = self.retry_delay * (2 ** attempt)  # 指数退避策略
                    print(f"等待 {wait_time} 秒后重试...")
                    time.sleep(wait_time)
                    
        print(f"错误: {city_name}在{formatted_time}第{page_num}页达到最大重试次数，跳过此页")
        return None

    def get_total_pages(self, city_id, time_param):
        """获取总页数"""
        html = self.get_page(city_id, time_param, 1)
        if not html:
            return 0
        soup = BeautifulSoup(html, 'html.parser')
        
        # 尝试从页面解析总页数
        total_pages_element = soup.select_one('body > div.container-body > div.rank-left-box > div.pageNation-box > div > a:nth-child(10)')
        if total_pages_element:
            try:
                total_pages = int(total_pages_element.text.strip())
                if total_pages >= 10:
                    return 10
                else:
                    return total_pages
            except ValueError:
                city_name = self.city_mapping.get(city_id, city_id)
                print(f"无法解析 {city_name} 在 {time_param} 的总页数，默认设为1页")
                
        # 如果无法解析总页数，默认设为0页
        return 0

    def parse_page(self, html, city_name, formatted_time):
        """解析页面内容，提取汽车销量数据"""
        if not html:
            return []

        data = []
        soup = BeautifulSoup(html, 'html.parser')

        # 遍历 1 到 10
        for i in range(1, 11):
            selector = f'body > div.container-body > div.rank-left-box > div.rk-list-box > div:nth-child({i})'
            car_items = soup.select(selector)
            for item in car_items:
                try:
                    # 提取车型名称
                    name_element = item.select_one('div.rk-car-info-box.db-i.v-al-m > div.rk-car-name')
                    car_name = name_element.text.strip() if name_element else "未知车型"

                    # 提取价格
                    price_element = item.select_one('div.rk-car-info-box.db-i.v-al-m > div.rk-car-price')
                    price = price_element.text.strip() if price_element else "未知价格"

                    # 提取销量
                    sales_element = item.select_one('div.rk-car-num-box.db-i.v-al-m > span')
                    sales = sales_element.text.strip() if sales_element else "0辆"

                    # 保存数据，添加时间和城市信息
                    data.append({
                        '时间': formatted_time,
                        '城市': city_name,
                        '车型名称': car_name,
                        '价格区间': price,
                        '销量': sales,
                    })
                except Exception as e:
                    print(f"解析车型数据时出错: {e}")
                    
        print(f"成功解析 {city_name} 在 {formatted_time} 的 {len(data)} 条数据")
        return data

    def save_to_csv(self, data):
        """将数据保存到统一的CSV文件"""
        if not data:
            print("没有数据可保存")
            return

        full_path = os.path.join('易车数据', self.output_file)
        df = pd.DataFrame(data)
        
        # 检查文件是否存在以决定是否写入表头
        file_exists = os.path.isfile(full_path)
        df.to_csv(full_path, mode='a', index=False, encoding='utf-8-sig', header=not file_exists)
        print(f"{len(data)} 条数据已追加到 {full_path}")

    def crawl_and_save_page(self, city_id, time_param, page_num):
        """爬取单页数据并保存"""
        city_name = self.city_mapping.get(city_id, city_id)
        formatted_time = self.format_time(time_param)
        
        print(f"开始爬取 {city_name} 在 {formatted_time} 第 {page_num} 页...")
        html = self.get_page(city_id, time_param, page_num)
        
        if html:
            page_data = self.parse_page(html, city_name, formatted_time)
            self.save_to_csv(page_data)
            
            # 随机延迟，避免请求过于频繁
            delay = random.uniform(0.1, 0.2)
            print(f"等待 {delay:.2f} 秒后继续...")
            time.sleep(delay)
            return True
        return False

    def crawl_all_pages(self, city_id, time_param):
        """爬取所有页面的数据"""
        city_name = self.city_mapping.get(city_id, city_id)
        formatted_time = self.format_time(time_param)
        
        total_pages = self.get_total_pages(city_id, time_param)
        print(f"开始爬取 {city_name} 在 {formatted_time} 的数据，总页数: {total_pages}")

        for page in range(1, total_pages + 1):
            success = self.crawl_and_save_page(city_id, time_param, page)
            
            if not success:
                print(f"警告: {city_name}在{formatted_time}第{page}页爬取失败，将继续尝试下一页")

        print(f"{city_name} 在 {formatted_time} 的数据爬取完成")

    def run(self):
        """运行爬虫"""
        start_time = datetime.now()
        print(f"爬虫启动时间: {start_time}")
            
        for city_id, city_name in self.city_mapping.items():
            for time_param in self.time_params:
                self.crawl_all_pages(city_id, time_param)
                
                # 爬取完一个城市的所有时间数据后，添加较长的延迟
                delay = random.uniform(0.5, 1)
                print(f"完成 {city_name} 的所有时间数据爬取，等待 {delay:.2f} 秒后继续下一个城市...")
                time.sleep(delay)
                
        end_time = datetime.now()
        print(f"爬虫完成时间: {end_time}")
        print(f"总运行时间: {end_time - start_time}")

if __name__ == "__main__":
    spider = YicheSalesSpider()
    spider.run()