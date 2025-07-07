SET hive.execution.engine=mr;
USE dws;
CREATE TABLE dws.dws_car_model_with_id AS
SELECT ROW_NUMBER() OVER (ORDER BY model_name) AS car_model_id,
       model_name,
       brand,
       level,
       launch_date,
       official_price,
       engine_type,
       seat_num,
       drive_type,
       range_km
FROM dws.dws_car_model;
