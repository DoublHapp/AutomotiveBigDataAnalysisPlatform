package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预测结果实体类
 * 对应数据库prediction表
 */
@Entity
@Table(name = "prediction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prediction {
    
    @Id
    @Column(name = "pred_id")
    private Long predId;
    
    @Column(name = "obj_type", length = 8)
    private String objType;
    
    @Column(name = "car_model_id", nullable = false)
    private Long carModelId;
    
    @Column(name = "region_id", nullable = false)
    private Long regionId;
    
    @Column(name = "period", length = 16)
    private String period;
    
    @Column(name = "model_type", length = 32)
    private String modelType;
    
    @Column(name = "model_param", length = 256)
    private String modelParam;
    
    @Column(name = "pred_result", columnDefinition = "TEXT")
    private String predResult;
    
    @Column(name = "fit_score", precision = 5, scale = 4)
    private BigDecimal fitScore;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
}
