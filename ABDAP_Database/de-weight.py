import pandas as pd

# 读取两个 CSV 文件
df1 = pd.read_csv('car_models.csv', encoding='GBK')
df2 = pd.read_csv('ods_car_config.csv', encoding='GBK')

# 分别按主键列去重
df1_unique = df1.drop_duplicates(subset=['model_name'])
df2_unique = df2.drop_duplicates(subset=['car_name'])

# 获取交集
common_names = set(df1_unique['model_name']) & set(df2_unique['car_name'])

# 只保留交集行
common_rows_df1 = df1_unique[df1_unique['model_name'].isin(common_names)]
common_rows_df2 = df2_unique[df2_unique['car_name'].isin(common_names)]

# 分别保存结果
common_rows_df1.to_csv('common_car_deweight_models.csv', index=False, encoding='GBK')
common_rows_df2.to_csv('common_ods_car_deweight_config.csv', index=False, encoding='GBK')

print('去重并筛选交集已完成，分别保存在 common_car_models.csv 和 common_ods_car_config.csv')
