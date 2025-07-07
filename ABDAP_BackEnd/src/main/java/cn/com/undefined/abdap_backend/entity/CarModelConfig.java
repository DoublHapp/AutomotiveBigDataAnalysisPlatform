package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * 车型配置实体类
 * 对应数据库car_model_config表
 */
@Entity
@Table(name = "car_model_config")
@IdClass(CarModelConfigId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarModelConfig {
    
    @Id
    @Column(name = "car_model_id")
    private Long carModelId;
    
    @Id
    @Column(name = "config_id")
    private Long configId;
    
    @Column(name = "enabled")
    private Byte enabled;
    
    @Column(name = "optional_price", precision = 10, scale = 2)
    private BigDecimal optionalPrice;
    
    @Column(name = "score")
    private Byte score;
}
