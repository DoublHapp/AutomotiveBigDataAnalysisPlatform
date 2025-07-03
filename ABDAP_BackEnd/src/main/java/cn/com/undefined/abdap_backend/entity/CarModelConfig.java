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
    
    // 建立与CarModel实体的关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id", insertable = false, updatable = false)
    private CarModel carModel;
    
    // 建立与Config实体的关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", insertable = false, updatable = false)
    private Config config;
    
    /**
     * 检查配置是否标配
     */
    public boolean isStandardConfig() {
        return enabled != null && enabled == 1;
    }
    
    /**
     * 检查配置是否选装
     */
    public boolean isOptionalConfig() {
        return enabled != null && enabled == 2;
    }
    
    /**
     * 检查配置是否无
     */
    public boolean isConfigNotAvailable() {
        return enabled == null || enabled == 0;
    }
    
    /**
     * 获取配置状态文本
     */
    public String getConfigStatusText() {
        if (enabled == null || enabled == 0) {
            return "无";
        } else if (enabled == 1) {
            return "标配";
        } else if (enabled == 2) {
            return "选装";
        }
        return "未知";
    }
    
    /**
     * 获取选装价格显示文本
     */
    public String getOptionalPriceText() {
        if (optionalPrice == null || optionalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return "免费";
        }
        return String.format("%.2f元", optionalPrice);
    }
    
    /**
     * 获取性能评分文本
     */
    public String getScoreText() {
        if (score == null) {
            return "未评分";
        }
        if (score >= 4) {
            return "优秀(" + score + "/5)";
        } else if (score >= 3) {
            return "良好(" + score + "/5)";
        } else if (score >= 2) {
            return "一般(" + score + "/5)";
        } else {
            return "较差(" + score + "/5)";
        }
    }
}
