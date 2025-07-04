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
}
