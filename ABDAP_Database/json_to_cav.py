import json
import csv

# 假设你的json数据存储在'cars.json'
with open('car_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

# 获取评分的所有项目（字段名会自动展开）
score_keys = list(data[0]['各项目评分'].keys())
score_fields = []
for k in score_keys:
    score_fields.append(f"{k}_current")
    score_fields.append(f"{k}_average")

# 定义csv字段
csv_fields = ["汽车名", "经销商报价", "厂商指导价"] + score_fields + ["汽车图片链接"]

with open('cars_sorce.csv', 'w', encoding='utf-8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=csv_fields)
    writer.writeheader()
    for car in data:
        row = {
            "汽车名": car.get("汽车名", ""),
            "经销商报价": car.get("经销商报价", ""),
            "厂商指导价": car.get("厂商指导价", ""),
            "汽车图片链接": car.get("汽车图片链接", ""),
        }
        for k in score_keys:
            row[f"{k}_current"] = car["各项目评分"][k].get("current", "")
            row[f"{k}_average"] = car["各项目评分"][k].get("average", "")
        writer.writerow(row)
