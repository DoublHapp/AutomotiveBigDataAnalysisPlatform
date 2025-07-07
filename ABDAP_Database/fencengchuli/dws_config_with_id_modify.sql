SET hive.execution.engine=mr;
INSERT OVERWRITE TABLE dws.dws_config_with_id
SELECT config_id,
       CASE
           WHEN config_name = '气囊' THEN '侧安全气帘'
           ELSE config_name
           END AS config_name,
       config_type,
       car_series_count
FROM dws.dws_config_with_id;
