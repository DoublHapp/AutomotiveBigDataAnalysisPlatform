from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
import csv
import time
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import re
# CSV_FILENAME = "car_model_config.csv"
# CSV_HEADERS = ["car_model_name", "config_name", "enabled","optional_price","sorce"]

def init_driver():
    options = webdriver.ChromeOptions()
    options.add_argument('--disable-blink-features=AutomationControlled')
    options.add_argument('--disable-infobars')
    options.add_argument('user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=options)
    return driver

import csv
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from bs4 import BeautifulSoup

def main():
    driver = init_driver()
    configs = [
        ("侧安全气帘", "front_rear_airbag"),
        ("胎压监测", "tire_pressure_system"),
        ("ESP车身稳定系统", "body_stability_system"),
        ("并线辅助", "line_support"),
        ("车道保持辅助系统", "lane_keeping_assist"),
        ("主动刹车/主动安全系统", "active_brake"),
        ("防爆轮胎", "explosion_tire"),
        ("儿童座椅接口ISOFIX", "child_seat_interface"),
        ("夜视系统", "night_vision_system"),
        ("道路交通标识识别", "road_traffic_sign_recognition"),
        ("被动行人保护", "passive_pedestrian_protection"),
        ("自动驻车", "auto_park"),
        ("自动泊车入位", "auto_park_entry"),
        ("上坡辅助", "uphill_support"),
        ("陡坡缓降", "steep_slope"),
        ("可变悬挂", "variable_suspension"),
        ("空气悬挂", "air_suspension"),
        ("可变转向比系统", "variable_steer_system"),
        ("整体主动转向系统", "overall_turn"),
        ("前桥限滑方式", "front_slip_method"),
        ("后桥限滑方式", "rear_slip_method"),
        ("中央差速器锁止", "central_differential_lock"),
        ("方向盘电动调节", "elec_steer_wheel_adjustment"),
    ]
    config_names = [c[0] for c in configs]
    fieldnames = ["car_name"] + config_names

    last_html = ""
    for i in range(20041, 20042):
        print(f"正在爬取第{i}项")
        url = f'https://www.dongchedi.com/auto/params-carIds-x-{i}'

        driver.get(url)
        try:
            # 等主表格出现
            WebDriverWait(driver, 1).until(
                EC.presence_of_element_located((By.CSS_SELECTOR, 'div.table_col__3Pc3_'))
            )
            # 等html内容和上一次不同，或车型数为0
            WebDriverWait(driver, 1).until(
                lambda d: d.page_source != last_html or
                          len(d.find_elements(By.CSS_SELECTOR, "a.cell_car__28WzZ.line-2")) == 0
            )
        except Exception as e:
            print(f"Page {i} failed to load or content not refreshed: {e}")
            continue

        html = driver.page_source
        if html == last_html:
            print(f"Page {i}: 页面HTML未变化，跳过")
            continue
        last_html = html  # 只要变了就更新

        soup = BeautifulSoup(html, "html.parser")
        car_cols = soup.find_all("a", attrs={"class": "cell_car__28WzZ line-2"})
        car_names = [car_col.get_text(strip=True).split("+")[0].strip() for car_col in car_cols]
        if not car_names:
            print(f"Page {i}: 没有车型数据，跳过")
            continue

        # 针对每个配置项，收集该页所有车型的配置
        all_config_points = {conf[0]: [] for conf in configs}
        for config_ch_name, anchor in configs:
            config_row = soup.find("div", attrs={"data-row-anchor": anchor})
            if not config_row:
                points = [None] * len(car_names)
            else:
                config_cols = config_row.find_all("div", class_="cell_normal__37nRi")
                points = []
                for col in config_cols:
                    span = col.find("span")
                    if not span:
                        points.append(None)
                    else:
                        v = span.get_text(strip=True)
                        if v == "●":
                            points.append("标配")
                        elif v == "—":
                            points.append("无")
                        elif v == "○":
                            points.append("选装")
                        elif v == "":
                            points.append(None)
                        else:
                            points.append(v)
                while len(points) < len(car_names):
                    points.append(None)
            all_config_points[config_ch_name] = points

        # 按车型组织，每台车一行，追加写入
        with open('ods_car_config.csv', 'a', newline='', encoding='GBK') as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            for idx, car_name in enumerate(car_names):
                row = {"car_name": car_name}
                for conf in config_names:
                    points = all_config_points[conf]
                    row[conf] = points[idx] if idx < len(points) else None
                # 拼接输出，内容按你写入CSV的顺序，全部用逗号分隔
                values = [row['car_name']] + [str(row[conf]) if row[conf] is not None else 'None' for conf in config_names]
                print(','.join(values))
                try:
                    writer.writerow(row)
                except UnicodeEncodeError as e:
                    print(f"编码错误，跳过该行: {row}，错误信息: {e}")
                    continue

if __name__ == "__main__":
    main()