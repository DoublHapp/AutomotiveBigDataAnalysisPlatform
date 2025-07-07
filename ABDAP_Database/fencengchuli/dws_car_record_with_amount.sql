SET hive.execution.engine=mr;
USE dws;
CREATE TABLE dws_car_record_with_amount AS
SELECT year,
       city,
       model_name,
       price_range,
       sales_count,
       -- 计算价格区间中间值（万），乘销量，得到销售金额（单位：万元）
       CAST(
               CASE
                   WHEN instr(price_range, '-') > 0
                       THEN (
                       (CAST(regexp_extract(price_range, '^([0-9.]+)', 1) AS DOUBLE) +
                        CAST(regexp_extract(price_range, '-([0-9.]+)', 1) AS DOUBLE)) / 2
                       )
                   ELSE CAST(regexp_extract(price_range, '^([0-9.]+)', 1) AS DOUBLE)
                   END
                   * CAST(sales_count AS BIGINT)
           AS BIGINT) AS sale_amount
FROM ods.ods_sale_record;
