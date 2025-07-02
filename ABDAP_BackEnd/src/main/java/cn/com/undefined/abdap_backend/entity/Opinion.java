package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * 口碑评价实体类
 * 对应数据库opinion表
 */
@Entity
@Table(name = "opinion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Opinion {
    
    @Id
    @Column(name = "opinion_id")
    private Long opinionId;
    
    @Column(name = "car_model_id", nullable = false)
    private Long carModelId;
    
    @Column(name = "score", precision = 3, scale = 1)
    private BigDecimal score;
      /**
     * 关联的车型（一对一关系）
     * 通过car_model_id外键建立关联
     * 每个车型只有一个综合的口碑评价记录
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id", insertable = false, updatable = false)
    private CarModel carModel;
    
    /**
     * 获取评分显示文本
     */
    public String getScoreDisplayText() {
        if (score == null) {
            return "暂无评分";
        }
        return String.format("%.1f分", score);
    }
    
    /**
     * 获取评分等级
     */
    public String getScoreLevel() {
        if (score == null) {
            return "未评分";
        }
        
        double scoreValue = score.doubleValue();
        if (scoreValue >= 4.5) {
            return "优秀";
        } else if (scoreValue >= 4.0) {
            return "良好";
        } else if (scoreValue >= 3.0) {
            return "一般";
        } else if (scoreValue >= 2.0) {
            return "较差";
        } else {
            return "很差";
        }
    }
    
    /**
     * 检查是否为高分评价
     */
    public boolean isHighScore() {
        return score != null && score.compareTo(new BigDecimal("4.0")) >= 0;
    }
    
    /**
     * 检查是否为低分评价
     */
    public boolean isLowScore() {
        return score != null && score.compareTo(new BigDecimal("3.0")) < 0;
    }
}
