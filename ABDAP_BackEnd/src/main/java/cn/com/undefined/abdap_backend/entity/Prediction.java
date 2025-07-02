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
    
    /**
     * 关联的车型（多对一关系）
     * 通过car_model_id外键建立关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id", insertable = false, updatable = false)
    private CarModel carModel;
    
    /**
     * 关联的地区（多对一关系）
     * 通过region_id外键建立关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", insertable = false, updatable = false)
    private Region region;
    
    /**
     * 获取预测对象类型显示文本
     */
    public String getObjTypeDisplayText() {
        if (objType == null || objType.trim().isEmpty()) {
            return "未知类型";
        }
        switch (objType.toLowerCase()) {
            case "sales":
                return "销量预测";
            case "price":
                return "价格预测";
            case "market":
                return "市场预测";
            default:
                return objType;
        }
    }
    
    /**
     * 获取预测周期显示文本
     */
    public String getPeriodDisplayText() {
        if (period == null || period.trim().isEmpty()) {
            return "未指定周期";
        }
        return period;
    }
    
    /**
     * 获取模型拟合度显示文本
     */
    public String getFitScoreDisplayText() {
        if (fitScore == null) {
            return "拟合度未知";
        }
        return String.format("%.2f%%", fitScore.multiply(new BigDecimal("100")));
    }
    
    /**
     * 获取模型拟合度等级
     */
    public String getFitScoreLevel() {
        if (fitScore == null) {
            return "未评估";
        }
        
        double scoreValue = fitScore.doubleValue();
        if (scoreValue >= 0.95) {
            return "优秀";
        } else if (scoreValue >= 0.85) {
            return "良好";
        } else if (scoreValue >= 0.70) {
            return "一般";
        } else if (scoreValue >= 0.50) {
            return "较差";
        } else {
            return "很差";
        }
    }
    
    /**
     * 检查是否为高拟合度模型
     */
    public boolean isHighFitScore() {
        return fitScore != null && fitScore.compareTo(new BigDecimal("0.85")) >= 0;
    }
    
    /**
     * 检查是否为低拟合度模型
     */
    public boolean isLowFitScore() {
        return fitScore != null && fitScore.compareTo(new BigDecimal("0.70")) < 0;
    }
    
    /**
     * 获取预测结果摘要（截取前100个字符）
     */
    public String getPredResultSummary() {
        if (predResult == null || predResult.trim().isEmpty()) {
            return "暂无预测结果";
        }
        if (predResult.length() <= 100) {
            return predResult;
        }
        return predResult.substring(0, 100) + "...";
    }
}
