import requests
from lxml import etree
import json
import os
from urllib.parse import urljoin
import sys
import time
from urllib.parse import urlparse
import csv

# 创建保存图片的目录
def create_save_directory():
    if not os.path.exists('car_images'):
        os.makedirs('car_images')
    if not os.path.exists('safety_config_images'):
        os.makedirs('safety_config_images')

# 从 JSON 文件中读取 URL 列表
def read_urls_from_json(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        print(f"读取 URL 列表文件时出错: {e}")
        sys.exit(1)

# 清理文件名，移除非法字符
def sanitize_filename(filename):
    invalid_chars = '<>:"/\\|?*'
    for char in invalid_chars:
        filename = filename.replace(char, '_')
    return filename

# 下载单个图片，带重试机制
def download_image(img_data, save_dir, max_retries=3):
    img_url = img_data['src']
    img_filename = img_data['filename']
    page_url = img_data['page_url']
    save_path = os.path.join(save_dir, img_filename)

    # 检查文件是否已存在
    if os.path.exists(save_path):
        print(f"跳过已下载的图片: {save_path}")
        return True

    for attempt in range(max_retries):
        try:
            print(f"尝试下载图片 {attempt + 1}/{max_retries}: {img_url}")

            # 设置请求头和超时
            headers = {
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
                'Referer': page_url  # 设置 Referer 头，避免防盗链
            }

            # 发送请求下载图片，设置超时时间
            response = requests.get(img_url, headers=headers, stream=True, timeout=30)
            response.raise_for_status()

            # 创建目录（如果不存在）
            os.makedirs(os.path.dirname(save_path), exist_ok=True)

            # 保存图片
            with open(save_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    if chunk:  # 过滤空块
                        f.write(chunk)

            print(f"已成功下载图片: {save_path}")
            return True

        except requests.exceptions.Timeout:
            print(f"下载超时 ({attempt + 1}/{max_retries}): {img_url}")
        except requests.exceptions.RequestException as e:
            print(f"下载请求错误 ({attempt + 1}/{max_retries}): {e} - {img_url}")
        except Exception as e:
            print(f"保存图片时出错 ({attempt + 1}/{max_retries}): {e} - {img_url}")

        # 重试前等待一段时间
        if attempt < max_retries - 1:
            wait_time = (attempt + 1) * 2  # 指数退避
            print(f"将在 {wait_time} 秒后重试...")
            time.sleep(wait_time)

    print(f"下载失败: {img_url}")
    return False

# 爬取单个网页的汽车名称、汽车图片和汽车安全配置图片
def crawl_page(url):
    try:
        # 设置请求头，模拟浏览器访问
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }

        # 发送 HTTP 请求
        response = requests.get(url, headers=headers)
        # 检查响应状态码
        response.raise_for_status()
        # 设置响应的编码
        response.encoding = response.apparent_encoding
        # 使用 lxml 的 etree 解析 HTML 内容
        html = etree.HTML(response.text)

        # 爬取汽车名称
        car_name_elements = html.xpath('/html/body/div[8]/div[1]/div[2]/div[1]')
        if not car_name_elements:
            raise ValueError(f"未在 {url} 上找到匹配的汽车名称元素")
        car_name = car_name_elements[0].text.strip() if car_name_elements[0].text else ""
        if not car_name:
            raise ValueError(f"未从 {url} 中提取到有效的汽车名称")

        # 爬取汽车图片
        car_img_elements = html.xpath('/html/body/div[8]/div[1]/div[1]/img')
        car_img_data = []
        for i, img in enumerate(car_img_elements):
            img_src = img.get('src')
            if not img_src:
                print(f"警告: 在 {url} 上找到的汽车图片元素没有 src 属性")
                continue
            if not img_src.startswith(('http:', 'https:')):
                img_src = urljoin(url, img_src)
            try:
                result = urlparse(img_src)
                if not all([result.scheme, result.netloc]):
                    print(f"警告: 在 {url} 上找到的无效汽车图片 URL: {img_src}")
                    continue
            except ValueError:
                print(f"警告: 在 {url} 上找到的无效汽车图片 URL: {img_src}")
                continue
            img_filename = f"{sanitize_filename(car_name)}.jpg"
            car_img_data.append({
                'src': img_src,
                'filename': img_filename,
                'page_url': url
            })

        # 爬取汽车安全配置图片
        safety_img_elements = html.xpath('/html/body/div[8]/div[3]/div[1]/div/img')
        safety_img_data = []
        for i, img in enumerate(safety_img_elements):
            img_src = img.get('src')
            if not img_src:
                print(f"警告: 在 {url} 上找到的汽车安全配置图片元素没有 src 属性")
                continue
            if not img_src.startswith(('http:', 'https:')):
                img_src = urljoin(url, img_src)
            try:
                result = urlparse(img_src)
                if not all([result.scheme, result.netloc]):
                    print(f"警告: 在 {url} 上找到的无效汽车安全配置图片 URL: {img_src}")
                    continue
            except ValueError:
                print(f"警告: 在 {url} 上找到的无效汽车安全配置图片 URL: {img_src}")
                continue
            img_filename = f"{sanitize_filename(car_name)}_安全配置.jpg"
            safety_img_data.append({
                'src': img_src,
                'filename': img_filename,
                'page_url': url
            })

        return {
            'car_name': car_name,
            'car_img_data': car_img_data,
            'safety_img_data': safety_img_data
        }

    except requests.RequestException as e:
        print(f"请求 {url} 时出错: {e}")
        raise
    except Exception as e:
        print(f"处理 {url} 时出错: {e}")
        raise

# 下载图片
def download_images(img_data_list, save_dir):
    failed_images = []
    for img_data in img_data_list:
        success = download_image(img_data, save_dir)
        if not success:
            failed_images.append(img_data)
    if failed_images:
        print(f"共有 {len(failed_images)} 张图片下载失败")
        for img in failed_images:
            print(f"  - {img['src']}")
        return False
    return True

# 保存汽车数据到 CSV 文件
def save_car_data(car_data_list, csv_file='car_names.csv'):
    if not car_data_list:
        print("没有汽车数据可保存")
        return
    try:
        with open(csv_file, 'w', newline='', encoding='utf-8-sig') as f:
            writer = csv.DictWriter(f, fieldnames=['name'])
            writer.writerows(car_data_list)
        print(f"已将汽车数据保存到 {csv_file}")
    except Exception as e:
        print(f"保存 CSV 文件时出错: {e}")

# 主函数
def main():
    file_path = 'c-ncap_urls.json'
    # 创建保存图片的目录
    create_save_directory()
    # 读取 URL 列表
    urls = read_urls_from_json(file_path)

    failed_pages = []
    all_car_data = []

    for url in urls:
        print(f"\n正在爬取 {url}")
        try:
            # 爬取单个网页的数据
            page_data = crawl_page(url)
            car_name = page_data['car_name']
            car_img_data = page_data['car_img_data']
            safety_img_data = page_data['safety_img_data']

            all_car_data.append({'name': car_name})

            print(f"从 {url} 中提取了汽车名称: {car_name}")
            print(f"从 {url} 中提取了 {len(car_img_data)} 张汽车图片")
            print(f"从 {url} 中提取了 {len(safety_img_data)} 张汽车安全配置图片")

            # 下载汽车图片
            success_car = download_images(car_img_data, 'car_images')
            # 下载汽车安全配置图片
            success_safety = download_images(safety_img_data, 'safety_config_images')

            if not (success_car and success_safety):
                failed_pages.append(url)

        except ValueError as e:
            print(f"错误: {e}")
            failed_pages.append(url)
            sys.exit(1)  # 按要求，未找到元素时退出程序
        except Exception as e:
            print(f"爬取过程中发生错误: {e}")
            failed_pages.append(url)
            # 这里不退出，继续处理其他页面

    # 保存所有汽车数据到 CSV 文件
    save_car_data(all_car_data)

    # 输出总结
    print("\n===== 爬取总结 =====")
    print(f"总网页数: {len(urls)}")
    print(f"成功爬取的网页数: {len(urls) - len(failed_pages)}")
    print(f"失败的网页数: {len(failed_pages)}")
    print(f"总共提取的汽车名称: {len(all_car_data)} 个")

    if failed_pages:
        print("\n失败的网页列表:")
        for page in failed_pages:
            print(f"  - {page}")

    if failed_pages:
        sys.exit(1)  # 如果有失败，返回错误码
    else:
        print("所有网页爬取完成，所有数据提取和图片下载成功")

if __name__ == "__main__":
    main()