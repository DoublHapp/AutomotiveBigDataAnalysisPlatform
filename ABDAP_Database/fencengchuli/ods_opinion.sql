USE ods;
CREATE EXTERNAL TABLE IF NOT EXISTS ods_opinion
(
    car_name           STRING,
    dealer_price       STRING,
    manufacturer_price STRING,
    score_current      STRING,
    score_average      STRING,
    appearance_current STRING,
    appearance_average STRING,
    interior_current   STRING,
    interior_average   STRING,
    config_current     STRING,
    config_average     STRING,
    space_current      STRING,
    space_average      STRING,
    comfort_current    STRING,
    comfort_average    STRING,
    control_current    STRING,
    control_average    STRING,
    power_current      STRING,
    power_average      STRING,
    image_url          STRING
)
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
    STORED AS TEXTFILE
    LOCATION '/data/auto/ods/opinion/';
