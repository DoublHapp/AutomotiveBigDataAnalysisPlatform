import pandas as pd
import os
import chardet

csv_files = [
    'ods_brank.csv',
    'ods_common_car_deweight_models.csv',
    'ods_car_salerecord.csv',
    'ods_config.csv',
    'ods_common_ods_car_deweight_config.csv',
    'cars_sorce.csv'
]

out_dir = './utf8_csv'
if not os.path.exists(out_dir):
    os.makedirs(out_dir)

for fname in csv_files:
    try:
        # 检测编码
        with open(fname, 'rb') as f:
            result = chardet.detect(f.read(4096))
        detected_encoding = result['encoding']
        print(f"{fname} 检测到编码: {detected_encoding}")

        # 尝试标准方式读取
        try:
            df = pd.read_csv(fname, encoding=detected_encoding)
        except Exception as e:
            print(f"标准方式失败，将采用容错模式读取: {fname}，错误：{e}")
            # 容错模式：跳过坏字符
            with open(fname, 'r', encoding=detected_encoding, errors='ignore') as fin:
                lines = fin.readlines()
            # pandas从字符串加载
            from io import StringIO

            df = pd.read_csv(StringIO(''.join(lines)))

        out_path = os.path.join(out_dir, fname)
        df.to_csv(out_path, index=False, encoding='utf-8')
        print(f'已转换: {fname} -> {out_path}')
    except Exception as e:
        print(f'转换失败: {fname}, 错误: {e}')
