SET hive.execution.engine=mr;
CREATE TABLE dwd.dwd_brand_with_factory AS
SELECT ROW_NUMBER() OVER (ORDER BY b.brand_name) AS brand_id, -- 自增主键
       b.brand_name,
       c.brand                                   AS factory,
       b.brand_logo
FROM dws.dws_brand b
         LEFT JOIN dwd.dws_car_model_with_names c
                   ON b.brand_name = TRIM(c.model_names)
;


