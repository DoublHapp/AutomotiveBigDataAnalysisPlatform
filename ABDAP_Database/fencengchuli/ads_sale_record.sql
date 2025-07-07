SET hive.execution.engine=mr;
CREATE TABLE ads.ads_sale_record
(
    sale_id      BIGINT,
    car_model_id BIGINT,
    region_id    BIGINT,
    sale_month   DATE,
    sale_count   INT,
    sale_amount  DECIMAL(12, 2)
)
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
    STORED AS TEXTFILE;
INSERT INTO ads.ads_sale_record
SELECT sale_id,
       car_model_id,
       region_id,
       -- 如果 sale_month 字段格式为'2023年02月'，需要转为 '2023-02-01' 格式
       CASE
           WHEN LENGTH(regexp_replace(sale_month, '[^0-9]', '')) = 6 THEN
               to_date(concat(substr(sale_month, 1, 4), '-', substr(sale_month, 6, 2), '-01'))
           WHEN LENGTH(regexp_replace(sale_month, '[^0-9]', '')) = 4 THEN
               to_date(concat(substr(sale_month, 1, 4), '-01-01'))
           ELSE
               NULL
           END AS sale_month,
       sale_count,
       CAST(sale_amount AS DECIMAL(12, 2))
FROM dws.sale_record;