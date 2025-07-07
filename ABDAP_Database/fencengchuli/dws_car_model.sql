USE dws;
CREATE EXTERNAL TABLE IF NOT EXISTS dws_car_model
(
    model_name        STRING,

    brand             STRING,
    level             STRING,
    launch_date       STRING,
    official_price    STRING,
    engine_type       STRING,
    seat_num          STRING,
    drive_type        STRING,
    range_km          STRING,
    Power_consumption STRING,
    Fuel_consumption  STRING


);
INSERT OVERWRITE TABLE dws_car_model
SELECT
       model_name,
       brand                                   AS brand_id, -- 直接用品牌名
       level,
       launch_date,
       official_price,
       engine_type,
       CAST(seat_num AS INT),
       drive_type,
       CAST(range_km AS INT),
       power_consumption,
       fuel_consumption
FROM (SELECT DISTINCT model_name,
                      brand,
                      level,
                      launch_date,
                      official_price,
                      engine_type,
                      seat_num,
                      drive_type,
                      range_km,
                      power_consumption,
                      fuel_consumption
      FROM ods.ods_car_model) t;
