USE ods;
CREATE EXTERNAL TABLE IF NOT EXISTS ods_sale_record
(
    year        STRING,
    city        STRING,
    model_name  STRING,
    price_range STRING,
    sales_count STRING
)
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
    STORED AS TEXTFILE
    LOCATION '/data/auto/ods/sale_record/';

