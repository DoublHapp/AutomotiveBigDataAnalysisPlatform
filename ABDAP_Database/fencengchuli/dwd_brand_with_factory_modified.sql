SET hive.execution.engine=mr;


CREATE TABLE dwd.dwd_unique_brand_factory AS
SELECT b.brand_name,
       c.brand AS factory,
       b.brand_logo
FROM dws.dws_brand b
         LEFT JOIN dwd.dws_car_model_with_names c
                   ON b.brand_name = TRIM(c.model_names)
GROUP BY b.brand_name,
         c.brand,
         b.brand_logo;

CREATE TABLE dwd.dwd_brand_with_factory_modified AS
SELECT ROW_NUMBER() OVER (ORDER BY brand_name, factory, brand_logo) AS brand_id,
       brand_name,
       factory,
       brand_logo
FROM dwd.dwd_unique_brand_factory;
