from bs4 import BeautifulSoup
from selenium import webdriver
from selenium.webdriver.common.by import By
import csv
import time
from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
import re
CSV_FILENAME = "config.csv"
CSV_HEADERS = ["config_name", "config_type", "car_series_count"]

def init_driver():
    options = webdriver.ChromeOptions()
    options.add_argument('--disable-blink-features=AutomationControlled')
    options.add_argument('--disable-infobars')
    options.add_argument('user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36')
    service = Service(ChromeDriverManager().install())
    driver = webdriver.Chrome(service=service, options=options)
    return driver

def main():
    driver = init_driver()
    url = 'https://www.dongchedi.com/auto/library-accurate/118x'
    all_rows = []

    for i in range(5):
        # 每次进入安全配置或内饰配置前都刷新一次页面，防止有累计条件
        driver.get(url)
        time.sleep(2)

        html = driver.page_source
        soup = BeautifulSoup(html, "html.parser")

        all_type = soup.findAll(
            "div",
            attrs={
                "class": "layout_root__3ftCy",
                "name": re.compile("^(safe|inner|accessibility|media|air_conditioning)$")
            }
        )

        safe_div = all_type[i]
        config_type = safe_div.find("span", attrs={"class": "layout_label__1qfS8"})
        config_names = safe_div.find("span", class_="layout_content__hziOT")
        items = config_names.find_all(["span", "a"], recursive=False)

        for item in items:
            config_name = item.text.strip()
            config_type_name = config_type.text.strip()
            print(config_name, config_type_name)

            # 重新定位滚动容器与可点击项（刷新后元素全部要重新查找）
            scroll_div = driver.find_element(By.ID, "scrollContainer")
            clickable = driver.find_element(
                By.XPATH,
                f"//span[contains(@class,'layout_content__hziOT')]//*[text()='{config_name}']"
            )

            # 滚动+点击
            driver.execute_script("""
                arguments[0].scrollTop = arguments[1].offsetTop - arguments[0].offsetTop;
            """, scroll_div, clickable)
            time.sleep(1)
            driver.execute_script("arguments[0].click();", clickable)
            time.sleep(1.2)

            # 获取车型数
            html = driver.page_source
            soup = BeautifulSoup(html, "html.parser")
            p_elem = soup.find("p", attrs={"class": "sort_matched__1HxFH"})
            if p_elem:
                count_elem = p_elem.find("span", attrs={"class": "sort_count__32REk"})
                if count_elem and count_elem.text.strip().isdigit():
                    car_count = int(count_elem.text.strip())
                else:
                    car_count = 0
                print("车型数：", car_count)
            else:
                print("没有找到目标p标签")
                car_count = 0

            all_rows.append({
                "config_name": config_name,
                "config_type": config_type_name,
                "car_series_count": car_count
            })

            print(f"{config_type_name} - {config_name} ：车型数 {car_count}")

            # 刷新页面准备采集下一个配置项
            driver.get(url)
            time.sleep(2)

    # 更新表头
    with open(CSV_FILENAME, 'w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=["config_name", "config_type", "car_series_count"])
        writer.writeheader()
        writer.writerows(all_rows)

    print(f"已写入{len(all_rows)}条配置到 {CSV_FILENAME}")
    driver.quit()


if __name__ == "__main__":
    main()