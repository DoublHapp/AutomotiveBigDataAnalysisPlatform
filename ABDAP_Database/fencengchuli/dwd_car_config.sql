SET hive.execution.engine=mr;
USE dwd;
CREATE TABLE IF NOT EXISTS dwd_car_model_config
(
    car_model_id   BIGINT,
    config_id      BIGINT,
    enabled        TINYINT,
    optional_price DECIMAL(10, 2),
    score          DECIMAL(2, 1)
)
    ROW FORMAT DELIMITED
        FIELDS TERMINATED BY ','
    STORED AS TEXTFILE;
INSERT OVERWRITE TABLE dwd_car_model_config
SELECT t.car_model_id,
       t.config_id,
       CAST(t.enabled AS TINYINT)                        AS enabled,
       CASE
           WHEN t.enabled = '2' THEN CAST(FLOOR(rand() * 2001 + 500) AS DECIMAL(10, 2))
           ELSE NULL
           END                                           AS optional_price,
       CAST(ROUND(rand() * 1.5 + 3, 1) AS DECIMAL(2, 1)) AS score
FROM (SELECT pair.car_model_id,
             pair.config_id,
             CASE pair.config_name
                 WHEN '侧安全气帘' THEN wide.side_airbag
                 WHEN '胎压监测' THEN wide.tire_pressure_monitor
                 WHEN 'ESP车身稳定系统' THEN wide.esp_stability_control
                 WHEN '并线辅助' THEN wide.lane_change_assist
                 WHEN '车道保持辅助系统' THEN wide.lane_keeping_assist
                 WHEN '主动刹车/主动安全系统' THEN wide.active_brake_safety
                 WHEN '防爆轮胎' THEN wide.run_flat_tire
                 WHEN '儿童座椅接口ISOFIX' THEN wide.isofix_child_seat
                 WHEN '夜视系统' THEN wide.night_vision_system
                 WHEN '道路交通标识识别' THEN wide.traffic_sign_recognition
                 WHEN '被动行人保护' THEN wide.pedestrian_protection
                 WHEN '自动驻车' THEN wide.auto_hold
                 WHEN '自动泊车入位' THEN wide.auto_parking
                 WHEN '上坡辅助' THEN wide.hill_start_assist
                 WHEN '陡坡缓降' THEN wide.hill_descent_control
                 WHEN '可变悬挂' THEN wide.adjustable_suspension
                 WHEN '空气悬挂' THEN wide.air_suspension
                 WHEN '可变转向比系统' THEN wide.variable_steering_ratio
                 WHEN '整体主动转向系统' THEN wide.integral_active_steering
                 WHEN '前桥限滑方式' THEN wide.front_axle_limited_slip
                 WHEN '后桥限滑方式' THEN wide.rear_axle_limited_slip
                 WHEN '中央差速器锁止' THEN wide.center_diff_lock
                 WHEN '方向盘电动调节' THEN wide.electric_steering_adjustment
                 ELSE NULL
                 END AS enabled
      FROM (SELECT m.car_model_id,
                   c.config_id,
                   m.model_name,
                   c.config_name
            FROM dws.dws_car_model_with_id m
                     CROSS JOIN dws.dws_config_with_id c) pair
               JOIN ods.ods_car_model_config wide
                    ON pair.model_name = wide.car_name) t
;