SET hive.execution.engine=mr;
CREATE TABLE ads.ads_region
(
    region_id     BIGINT,
    region_name   STRING,
    parent_region STRING
)
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
    STORED AS TEXTFILE;

INSERT INTO ads.ads_region
SELECT CAST(region_id AS BIGINT),
       region_name,
       parent_region
FROM ods.ods_region
WHERE CAST(region_id AS STRING) RLIKE '^[0-9]+$'
  AND region_id IS NOT NULL
  AND region_name != 'region_name'; -- 排除表头内容