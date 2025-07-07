SET hive.execution.engine=mr;

CREATE TABLE IF NOT EXISTS ads.ads_car_model_config
(
    car_model_id   BIGINT,
    config_id      BIGINT,
    enabled        TINYINT,
    optional_price DECIMAL(10, 2),
    score          TINYINT
);

INSERT OVERWRITE TABLE ads.ads_car_model_config
SELECT car_model_id,
       config_id,
       enabled,
       optional_price,
       CAST(score AS TINYINT) AS score
FROM dwd.dwd_car_model_config;
