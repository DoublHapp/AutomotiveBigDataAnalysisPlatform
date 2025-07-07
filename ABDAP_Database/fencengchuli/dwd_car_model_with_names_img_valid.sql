SET hive.execution.engine=mr;
CREATE TABLE dwd.dwd_car_model_with_names_img_valid AS
SELECT *
FROM dwd.dwd_car_model_with_names_img
WHERE image_url IS NOT NULL
  AND image_url != '';
