USE dws;
CREATE TABLE IF NOT EXISTS dws_opinion
(
    car_name           STRING,
    score_current      STRING,
    score_average      STRING,
    appearance_current STRING,
    appearance_average STRING,
    interior_current   STRING,
    interior_average   STRING,
    config_current     STRING,
    config_average     STRING,
    space_current      STRING,
    space_average      STRING,
    comfort_current    STRING,
    comfort_average    STRING,
    control_current    STRING,
    control_average    STRING,
    power_current      STRING,
    power_average      STRING,
    image_url          STRING
);
USE dws;
INSERT OVERWRITE TABLE dws_opinion
SELECT car_name,
       score_current,
       score_average,
       appearance_current,
       appearance_average,
       interior_current,
       interior_average,
       config_current,
       config_average,
       space_current,
       space_average,
       comfort_current,
       comfort_average,
       control_current,
       control_average,
       power_current,
       power_average,
       image_url
FROM ods.ods_opinion;


