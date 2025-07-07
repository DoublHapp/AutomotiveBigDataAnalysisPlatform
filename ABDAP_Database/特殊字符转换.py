import pandas as pd
from sqlalchemy import create_engine

# 配置数据库信息
host = 'localhost'
port = 3306
user = 'root'
password = 'root'
database = 'car_basic'
table = 'region'

# 创建数据库连接
engine = create_engine(f'mysql+pymysql://{user}:{password}@{host}:{port}/{database}?charset=utf8mb4')

# 读取region表
df = pd.read_sql(f'SELECT * FROM {table}', engine)

# 保存为CSV文件
df.to_csv('region.csv', index=False, encoding='utf-8-sig')   # 防止中文乱码

print('region.csv 已生成')
