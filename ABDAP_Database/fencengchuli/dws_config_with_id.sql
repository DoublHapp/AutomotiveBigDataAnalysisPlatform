SET hive.execution.engine=mr;
USE dws;
-- 1. 新建带自增ID的目标表

-- 2. 用 row_number() 生成自增id插入
INSERT INTO dws.dws_config_with_id
SELECT row_number() OVER (ORDER BY config_name) AS config_id,
       config_name,
       config_type,
       car_series_count
FROM ods.ods_config;
