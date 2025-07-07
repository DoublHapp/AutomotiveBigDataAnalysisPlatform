USE ods;
CREATE EXTERNAL TABLE IF NOT EXISTS ods_car_model_config
(
    `car_name`                     STRING,
    `side_airbag`                  STRING,
    `tire_pressure_monitor`        STRING,
    `esp_stability_control`        STRING,
    `lane_change_assist`           STRING,
    `lane_keeping_assist`          STRING,
    `active_brake_safety`          STRING,
    `run_flat_tire`                STRING,
    `isofix_child_seat`            STRING,
    `night_vision_system`          STRING,
    `traffic_sign_recognition`     STRING,
    `pedestrian_protection`        STRING,
    `auto_hold`                    STRING,
    `auto_parking`                 STRING,
    `hill_start_assist`            STRING,
    `hill_descent_control`         STRING,
    `adjustable_suspension`        STRING,
    `air_suspension`               STRING,
    `variable_steering_ratio`      STRING,
    `integral_active_steering`     STRING,
    `front_axle_limited_slip`      STRING,
    `rear_axle_limited_slip`       STRING,
    `center_diff_lock`             STRING,
    `electric_steering_adjustment` STRING
)
    ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
    STORED AS TEXTFILE
    LOCATION '/data/auto/ods/car_model_config/';
