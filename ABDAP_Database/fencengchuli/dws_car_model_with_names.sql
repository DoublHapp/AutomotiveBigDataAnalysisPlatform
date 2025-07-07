SET hive.execution.engine=mr;
USE dwd;

CREATE TABLE dws_car_model_with_names AS
SELECT car_model_id,
       model_name,
       brand,
       level,
       launch_date,
       official_price,
       engine_type,
       seat_num,
       drive_type,
       range_km,
       Power_consumption,
       Fuel_consumption,

       CASE
           WHEN model_name REGEXP '^\\d{4}款'
               THEN '' -- 以“xxxx款”开头，直接给空
           WHEN model_name REGEXP '\\d{4}款'
               THEN regexp_extract(model_name, '^(.*?)\\d{4}款', 1) -- 取第一个xxxx款之前内容
           ELSE model_name
           END AS model_names
FROM dws.dws_car_model_with_id;
