SET hive.execution.engine=mr;
CREATE TABLE dwd.dws_car_model_with_names_img
AS
SELECT a.*,
       b.image_url
FROM dwd.dws_car_model_with_names a
         LEFT JOIN
     dws.dws_opinion b
     ON
         TRIM(a.model_names) = TRIM(b.car_name);
