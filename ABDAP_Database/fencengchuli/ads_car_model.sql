SET hive.execution.engine=mr;
CREATE TABLE IF NOT EXISTS ads.car_model
(
    car_model_id   BIGINT, -- 主键，沿用原car_model_id
    model_name     STRING,
    brand_id       BIGINT,
    level          STRING,
    launch_date    STRING,
    official_price DECIMAL(10, 2),
    engine_type    STRING,
    seat_num       INT,
    drive_type     STRING,
    range_km       INT,
    model_names    STRING,
    image_url      STRING
);

INSERT OVERWRITE TABLE ads.car_model
SELECT a.car_model_id,
       a.model_name,
       b.brand_id,
       a.level,
       a.launch_date,
       CAST(regexp_replace(a.official_price, '[^0-9.]', '') AS DECIMAL(10, 2)) AS official_price,
       a.engine_type,
       CAST(regexp_extract(a.seat_num, '[0-9]+', 0) AS INT)                    AS seat_num,
       a.drive_type,
       CAST(regexp_extract(a.range_km, '[0-9]+', 0) AS INT)                    AS range_km,
       TRIM(a.model_names)                                                     AS model_names, -- 去掉前后空格
       a.image_url
FROM dwd.dwd_car_model_with_names_img_valid a
         LEFT JOIN
     dwd.dwd_brand_with_factory_modified b
     ON TRIM(a.model_names) = TRIM(b.brand_name);
