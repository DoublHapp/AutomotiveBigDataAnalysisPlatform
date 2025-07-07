USE ods;
CREATE EXTERNAL TABLE IF NOT EXISTS ods_config
(
    `config_name`      STRING,
    `config_type`      STRING,
    `car_series_count` STRING
)
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
    STORED AS TEXTFILE
    LOCATION '/data/auto/ods/config_heat/';
