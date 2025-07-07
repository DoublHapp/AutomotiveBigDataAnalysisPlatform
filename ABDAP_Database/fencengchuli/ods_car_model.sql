USE ods;
CREATE EXTERNAL TABLE IF NOT EXISTS ods_car_model
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
    Power_consumption string,
    Fuel_consumption  string


)
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
    STORED AS TEXTFILE
    LOCATION '/data/auto/ods/car_model/';