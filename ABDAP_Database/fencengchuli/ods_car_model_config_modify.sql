SET hive.execution.engine=mr;
INSERT OVERWRITE TABLE ods.ods_car_model_config
SELECT car_name,
       side_airbag,
       tire_pressure_monitor,
       esp_stability_control,
       lane_change_assist,
       lane_keeping_assist,
       CASE
           WHEN active_brake_safety = '主动刹车_主动安全系统' THEN '主动刹车/主动安全系统'
           ELSE active_brake_safety
           END AS active_brake_safety,
       run_flat_tire,
       isofix_child_seat,
       night_vision_system,
       traffic_sign_recognition,
       pedestrian_protection,
       auto_hold,
       auto_parking,
       hill_start_assist,
       hill_descent_control,
       adjustable_suspension,
       air_suspension,
       variable_steering_ratio,
       integral_active_steering,
       front_axle_limited_slip,
       rear_axle_limited_slip,
       center_diff_lock,
       electric_steering_adjustment
FROM ods.ods_car_model_config;
