import requests
from lxml import etree
import os
import time
from urllib.parse import urlparse

def crawl_car_info(url, max_retries=3):
    """爬取汽车信息并保存，添加重试机制"""
    retries = 0
    while retries < max_retries:
        try:
            # 发送请求获取页面内容
            headers = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36'
            }
            response = requests.get(url, headers=headers, timeout=10)
            response.raise_for_status()  # 检查请求是否成功

            # 处理响应编码
            if 'content-type' in response.headers and 'charset' in response.headers['content-type']:
                response.encoding = response.headers['content-type'].split('charset=')[-1]
            else:
                response.encoding = response.apparent_encoding

            # 解析HTML
            html = etree.HTML(response.text)

            # 爬取汽车名称
            name_xpath = '/html/body/div[1]/div/div[2]/div[2]/div[1]/div[1]/div[1]/h1'
            name_element = html.xpath(name_xpath)
            car_name = name_element[0].text.strip() if name_element else "未知车型"

            # 爬取商标LOGO图片链接
            logo_xpath = '/html/body/div[1]/div/div[2]/div[2]/div[1]/div[1]/div[1]/img'
            logo_element = html.xpath(logo_xpath)
            logo_url = logo_element[0].get('src') if logo_element else None

            return car_name, logo_url

        except requests.RequestException as e:
            print(f"请求出错: {e}，第 {retries + 1} 次重试...")
            retries += 1
            time.sleep(1)
        except Exception as e:
            print(f"爬取过程出错: {e}，第 {retries + 1} 次重试...")
            retries += 1
            time.sleep(1)

    print("达到最大重试次数，爬取失败")
    return None, None

def save_to_file(car_name, logo_url, save_file="car_info.csv"):
    """将信息以两列属性格式保存到CSV文件"""
    try:
        import csv
        file_exists = os.path.isfile(save_file)
        with open(save_file, 'a', encoding='utf-8', newline='') as f:
            writer = csv.writer(f)
            if not file_exists:
                writer.writerow(['汽车名称', '商标LOGO链接'])
            writer.writerow([car_name, logo_url])
        print(f"信息已保存至: {save_file}")
        return True
    except Exception as e:
        print(f"文件保存出错: {e}")
        return False

def main():
    # 目标URL
    for i in range(20012, 20042):
        target_url = f"https://www.dongchedi.com/auto/series/{i}"
        time.sleep(0.1)
        print("开始爬取汽车信息...")
        car_name, logo_url = crawl_car_info(target_url)
        if car_name and logo_url:
            print(f"爬取成功: {car_name}")
            # 保存信息到文件
            save_to_file(car_name, logo_url)
        else:
            print("汽车信息爬取失败")

if __name__ == "__main__":
    main()