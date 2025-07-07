USE dws;
CREATE TABLE IF NOT EXISTS dws_brand
(
    brand_id   BIGINT, -- 主键
    brand_name VARCHAR(64),
    factory    VARCHAR(64),
    brand_logo VARCHAR(256)
)
    STORED AS ORC;
INSERT OVERWRITE TABLE dws_brand
SELECT null     as brand_id,
       car_name as brand_name,
       null     as factory,
       logo_url as brand_logo
FROM ods.ods_car_logo;