USE ods;
CREATE EXTERNAL TABLE IF NOT EXISTS ods_car_logo
(
    car_name STRING, -- 汽车名称
    logo_url STRING  -- 商标LOGO链接
)
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
    STORED AS TEXTFILE
    LOCATION '/data/auto/ods/brank/';