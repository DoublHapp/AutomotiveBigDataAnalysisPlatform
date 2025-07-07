SET hive.execution.engine=mr;
CREATE TABLE IF NOT EXISTS dwd.dwd_fuel_economy
(
    fuel_id      BIGINT,
    car_model_id BIGINT,
    fuel_type    VARCHAR(16),
    avg_fuel     DECIMAL(5, 2),
    sample_count INT,
    collect_time DATE
);

-- 2. 插入数据
INSERT OVERWRITE TABLE dwd.dwd_fuel_economy
SELECT ROW_NUMBER() OVER (ORDER BY car_model_id) AS fuel_id,
       car_model_id,
       engine_type                               AS fuel_type,
       CAST(
               CASE
                   WHEN power_consumption IS NOT NULL AND TRIM(power_consumption) != '' THEN power_consumption
                   WHEN fuel_consumption IS NOT NULL AND TRIM(fuel_consumption) != '' THEN fuel_consumption
                   ELSE '1.5'
                   END AS DECIMAL(5, 2)
       )                                         AS avg_fuel,
       700                                       AS sample_count, -- 固定值，如需随机需用UDF/Python
       CURRENT_DATE                              AS collect_time  -- 系统当前日期
FROM dws.dws_car_model_with_id;
