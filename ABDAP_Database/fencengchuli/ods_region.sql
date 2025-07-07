CREATE TABLE IF NOT EXISTS ods.ods_region
(
    region_id     BIGINT,
    region_name   STRING,
    parent_region STRING
)
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
    STORED AS TEXTFILE
    LOCATION '/data/auto/ods/region/';