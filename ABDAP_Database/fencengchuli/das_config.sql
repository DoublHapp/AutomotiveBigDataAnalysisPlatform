SET hive.execution.engine=mr;

CREATE TABLE IF NOT EXISTS ads.ads_config
(
    config_id   BIGINT,
    config_name VARCHAR(64),
    config_type VARCHAR(32)
);
INSERT OVERWRITE TABLE ads.ads_config
SELECT config_id,
       config_name,
       config_type
FROM dws.dws_config_with_id;
