USE dws;
INSERT OVERWRITE TABLE dws.sale_record
SELECT ROW_NUMBER() OVER (ORDER BY rec.model_name, rec.city, rec.year) AS sale_id,
       model.car_model_id,
       region.region_id,
       rec.year,
       rec.sales_count                                                 AS sale_count,
       rec.sale_amount
FROM dws.dws_car_record_with_amount rec
         JOIN
     dwd.dws_car_model_with_names model
     ON model.model_names = rec.model_name
         JOIN
     ods.ods_region region
     ON rec.city = region.region_name;
