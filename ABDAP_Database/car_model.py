from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
import csv
import time
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
import re
import random
def init_driver():
    options = webdriver.ChromeOptions()
    options.add_argument('--disable-blink-features=AutomationControlled')
    options.add_argument('--disable-infobars')
    options.add_argument('user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=options)
    return driver



import re
import time
import csv
from bs4 import BeautifulSoup
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

def get_row_texts(soup, anchor, cell_class, expected_len):
    row = soup.find("div", attrs={"data-row-anchor": anchor})
    if row:
        cells = row.find_all("div", class_=cell_class)
        values = [cell.get_text(strip=True) for cell in cells]
        while len(values) < expected_len:
            values.append("")
        return values
    else:
        return [""] * expected_len

def safe_get(driver, url, max_retry=1, wait_time=2):
    """
    用driver打开url，最多重试max_retry次
    """
    for attempt in range(1, max_retry + 1):
        try:
            driver.get(url)
            WebDriverWait(driver, wait_time).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, 'div.table_col__3Pc3_'))
            )
            return True
        except Exception as e:
            print(f"第{attempt}次请求失败: {url}，错误：{e}")
            time.sleep(random.uniform(2, 5))
    return False

def main():
    driver = init_driver()
    failed_ids = []
    # with open("car_models.csv", "w", newline="", encoding="utf-8") as f:
    #     writer = csv.writer(f)
    #     writer.writerow([
    #         "model_name", "brank", "level", "launch_data",
    #         "official_price", "engine_type", "seat_num", "drive_type", "range_km", "Power consumption", "Fuel consumption"
    #     ])
    with open("car_models.csv", "a", newline="", encoding="GBK") as f:  # 追加模式
        writer = csv.writer(f)

        for i in range(9689,20041):
            url = f'https://www.dongchedi.com/auto/params-carIds-x-{i}'
            try:
                ok = safe_get(driver, url, max_retry=1, wait_time=2)
                if not ok:
                    print(f"最终失败，跳过id={i}")
                    failed_ids.append(i)
                    continue

                html = driver.page_source
                soup = BeautifulSoup(html, "html.parser")

                # 品牌
                brand_row = soup.find("div", attrs={"data-row-anchor": "sub_brand_name"})
                brand_cells = brand_row.find_all("div", class_="cell_normal__37nRi") if brand_row else []
                brands = [cell.get_text(strip=True) for cell in brand_cells]
                col_num = len(brands)

                # 车型名
                car_cols = soup.find_all("div", attrs={"class": "table_col__3Pc3_ table_is-head-col__1sAQG"})
                car_names = []
                for car_col in car_cols:
                    text = car_col.get_text(strip=True)
                    name = re.split(r"\+", text)[0].strip()
                    car_names.append(name)
                while len(car_names) < col_num:
                    car_names.append("")

                levels = get_row_texts(soup, "jb", "cell_normal__37nRi", col_num)
                times = get_row_texts(soup, "market_time", "cell_normal__37nRi", col_num)
                prices = get_row_texts(soup, "official_price", "cell_official-price__1O2th", col_num)
                engins = get_row_texts(soup, "fuel_form", "cell_normal__37nRi", col_num)
                seats = get_row_texts(soup, "seat_count", "cell_normal__37nRi", col_num)
                drives = get_row_texts(soup, "driver_form", "cell_normal__37nRi", col_num)
                kms = get_row_texts(soup, "recharge_mileage", "cell_normal__37nRi", col_num)
                Power_consumptions = get_row_texts(soup, "e_energy_equivalent_fuel_consumption", "cell_normal__37nRi", col_num)
                Fuel_consumptions = get_row_texts(soup, "wltc_fuel_comprehensive", "cell_normal__37nRi", col_num)

                seats = [s + "座" if s else "" for s in seats]

                for model_name, brank, level, launch_data, official_price, engine_type, seat_num, drive_type, range_km, Power_consumption, Fuel_consumption in zip(
                    car_names, brands, levels, times, prices, engins, seats, drives, kms, Power_consumptions, Fuel_consumptions
                ):
                    print(model_name, brank, level, launch_data, official_price, engine_type, seat_num, drive_type, range_km, Power_consumption, Fuel_consumption)
                    writer.writerow([
                        model_name, brank, level, launch_data,
                        official_price, engine_type, seat_num, drive_type, range_km, Power_consumption, Fuel_consumption
                    ])
                print(f"id={i} 爬取成功")
            except Exception as e:
                print(f"数据解析或保存失败，id={i}，错误：{e}")
                failed_ids.append(i)
    driver.quit()
    # 保存失败id
    if failed_ids:
        with open("failed_ids.txt", "w") as ff:
            ff.write('\n'.join(map(str, failed_ids)))
        print(f"共失败 {len(failed_ids)} 个，已记录到 failed_ids.txt")

if __name__ == "__main__":
    main()
