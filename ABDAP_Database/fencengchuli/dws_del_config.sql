SET hive.execution.engine=mr;
INSERT OVERWRITE TABLE dws.dws_config_with_id
SELECT *
FROM dws.dws_config_with_id
WHERE config_id != 2;


