SET hive.execution.engine=mr;
CREATE TABLE dwd.dwd_opinion_with_id AS
SELECT o.*,
       m.car_model_id
FROM dws.dws_opinion o
         LEFT JOIN dwd.dws_car_model_with_names m
                   ON TRIM(o.car_name) = TRIM(m.model_names);
