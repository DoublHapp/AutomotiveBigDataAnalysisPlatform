CREATE DATABASE IF NOT EXISTS car_basic DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 选择数据库
USE car_basic;
CREATE TABLE user (
    user_id     BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(64) NOT NULL COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码',
    mobile      VARCHAR(20) COMMENT '手机号',
    email       VARCHAR(64) COMMENT '邮箱',
    reg_time    DATETIME COMMENT '注册时间',
    role        VARCHAR(64) COMMENT '用户角色'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- 2. 品牌与车型

CREATE TABLE brand (
    brand_id    BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '品牌ID',
    brand_name  VARCHAR(64) NOT NULL COMMENT '品牌名称',
    factory     VARCHAR(64) COMMENT '所属厂商',
    brand_logo  VARCHAR(256) COMMENT '品牌LOGO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆品牌信息';

CREATE TABLE car_model (
    car_model_id   BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '车型ID',
    model_name     VARCHAR(64) NOT NULL COMMENT '车型名称',
    brand_id       BIGINT NOT NULL COMMENT '品牌ID',
    level          VARCHAR(32) COMMENT '级别',
    launch_date    DATE COMMENT '上市时间',
    official_price DECIMAL(10,2) COMMENT '官方指导价',
    engine_type    VARCHAR(32) COMMENT '动力类型',
    seat_num       INT COMMENT '最大乘坐人数',
    body_type      VARCHAR(32) COMMENT '车身类型',
    drive_type     VARCHAR(16) COMMENT '驱动方式',
    range_km       INT COMMENT '最大续航',
    KEY idx_brand_id (brand_id),
    CONSTRAINT fk_car_model_brand FOREIGN KEY (brand_id) REFERENCES brand(brand_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车型基础信息';

-- 3. 配置项与车型配置多对多

CREATE TABLE config (
    config_id    BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置项ID',
    config_name  VARCHAR(64) NOT NULL COMMENT '配置名称',
    config_type  VARCHAR(32) COMMENT '配置类型'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置项基础信息';

CREATE TABLE car_model_config (
    car_model_id    BIGINT NOT NULL COMMENT '车型ID',
    config_id       BIGINT NOT NULL COMMENT '配置项ID',
    enabled         TINYINT COMMENT '是否标配（1=标配，2=选装，0=无）',
    optional_price  DECIMAL(10,2) COMMENT '选装价格',
    score           TINYINT COMMENT '性能打分，1-5',
    PRIMARY KEY (car_model_id, config_id),
    KEY idx_config_id (config_id),
    CONSTRAINT fk_cmc_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id),
    CONSTRAINT fk_cmc_config FOREIGN KEY (config_id) REFERENCES config(config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车型与配置项关联';

-- 4. 地区

CREATE TABLE region (
    region_id        BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '地区ID',
    region_name      VARCHAR(64) NOT NULL COMMENT '地区名称',
    parent_region_id BIGINT DEFAULT NULL COMMENT '上级地区ID',
    KEY idx_parent_region (parent_region_id),
    CONSTRAINT fk_region_parent FOREIGN KEY (parent_region_id) REFERENCES region(region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地区信息';

-- 5. 销售与分析相关

CREATE TABLE sale_record (
    sale_id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '销售记录ID',
    car_model_id BIGINT NOT NULL COMMENT '车型ID',
    region_id    BIGINT NOT NULL COMMENT '地区ID',
    sale_month   DATE NOT NULL COMMENT '销售月份',
    sale_count   INT COMMENT '销售数量',
    sale_amount  DECIMAL(12,2) COMMENT '销售金额',
    KEY idx_model_region_month (car_model_id, region_id, sale_month),
    CONSTRAINT fk_sale_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id),
    CONSTRAINT fk_sale_region FOREIGN KEY (region_id) REFERENCES region(region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车型销售数据';

CREATE TABLE config_heat (
    heat_id        BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '热度ID',
    config_id      BIGINT NOT NULL COMMENT '配置项ID',
    collect_time   DATE NOT NULL COMMENT '统计时间',
    select_count   INT COMMENT '有该配置的车型数量',
    KEY idx_config_time (config_id, collect_time),
    CONSTRAINT fk_heat_config FOREIGN KEY (config_id) REFERENCES config(config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配置项热度';

CREATE TABLE fuel_economy (
    fuel_id       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '油耗ID',
    car_model_id  BIGINT NOT NULL COMMENT '车型ID',
    fuel_type     VARCHAR(16) COMMENT '动力类型',
    avg_fuel      DECIMAL(5,2) COMMENT '平均油耗',
    sample_count  INT COMMENT '样本数',
    collect_time  DATE COMMENT '统计时间',
    KEY idx_fuel_model_time (car_model_id, collect_time),
    CONSTRAINT fk_fuel_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车型油耗数据';

CREATE TABLE opinion (
    opinion_id    BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '口碑ID',
    car_model_id  BIGINT NOT NULL COMMENT '车型ID',
    score         DECIMAL(3,1) COMMENT '评分',
    KEY idx_opinion_model (car_model_id),
    CONSTRAINT fk_opinion_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车型口碑评分';

CREATE TABLE prediction (
    pred_id       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预测ID',
    obj_type      VARCHAR(8) COMMENT '预测对象类型',
    car_model_id  BIGINT NOT NULL COMMENT '车型ID',
    region_id     BIGINT NOT NULL COMMENT '地区ID',
    period        VARCHAR(16) COMMENT '预测周期',
    model_type    VARCHAR(32) COMMENT '模型类型',
    model_param   VARCHAR(256) COMMENT '模型参数',
    pred_result   TEXT COMMENT '预测结果',
    fit_score     DECIMAL(5,4) COMMENT '拟合优度',
    create_time   DATETIME COMMENT '创建时间',
    KEY idx_pred_model_region (car_model_id, region_id),
    CONSTRAINT fk_pred_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id),
    CONSTRAINT fk_pred_region FOREIGN KEY (region_id) REFERENCES region(region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售/市场预测分析';

CREATE TABLE competitor_analysis (
    compare_id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '竞品对比ID',
    car_model_id         BIGINT NOT NULL COMMENT '本车型ID',
    competitor_model_id  BIGINT NOT NULL COMMENT '竞品车型ID',
    compare_time         DATETIME COMMENT '对比时间',
    compare_content      TEXT COMMENT '对比内容',
    report_file          VARCHAR(256) COMMENT '报告文件路径',
    KEY idx_comp_model (car_model_id),
    KEY idx_comp_competitor (competitor_model_id),
    CONSTRAINT fk_comp_model FOREIGN KEY (car_model_id) REFERENCES car_model(car_model_id),
    CONSTRAINT fk_comp_competitor FOREIGN KEY (competitor_model_id) REFERENCES car_model(car_model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车型竞品对比分析';

-- 6. 排行榜

CREATE TABLE ranking (
    ranking_id   BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '榜单ID',
    rank_type    VARCHAR(32) NOT NULL UNIQUE COMMENT '榜单类型',
    rank_time    DATE NOT NULL COMMENT '榜单生成时间',
    ranking_data LONGTEXT NOT NULL COMMENT '榜单完整内容（如JSON）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='榜单缓存表';

-- 7. 购车推荐功能

CREATE TABLE purchase_survey (
    survey_id        BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '问卷ID',
    user_id          BIGINT COMMENT '用户ID，可为空',
    fill_time        DATETIME COMMENT '问卷填写时间',
    brand_pref       VARCHAR(256) COMMENT '品牌倾向',
    daily_mileage    VARCHAR(32) COMMENT '日均行驶里程',
    seat_num         VARCHAR(16) COMMENT '乘坐人数',
    energy_pref      VARCHAR(32) COMMENT '能源偏好',
    buy_priority     VARCHAR(64) COMMENT '购车优先级',
    budget_range     VARCHAR(32) COMMENT '预算范围',
    appearance_pref  VARCHAR(128) COMMENT '外观偏好',
    drive_type       VARCHAR(16) COMMENT '驱动形式',
    KEY idx_user_id (user_id),
    CONSTRAINT fk_survey_user FOREIGN KEY (user_id) REFERENCES user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购车推荐问卷';

CREATE TABLE recommend_history (
    recommend_id     BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐ID',
    survey_id        BIGINT NOT NULL COMMENT '问卷ID',
    user_id          BIGINT COMMENT '用户ID，可为空',
    recommend_time   DATETIME COMMENT '推荐时间',
    car_model_ids    VARCHAR(128) COMMENT '推荐车型ID列表',
    KEY idx_rec_user (user_id),
    KEY idx_rec_survey (survey_id),
    CONSTRAINT fk_rec_survey FOREIGN KEY (survey_id) REFERENCES purchase_survey(survey_id),
    CONSTRAINT fk_rec_user FOREIGN KEY (user_id) REFERENCES user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐历史';
