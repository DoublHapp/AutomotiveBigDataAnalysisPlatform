package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 油耗经济性实体类
 * 对应数据库fuel_economy表
 */
@Entity
@Table(name = "fuel_economy")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuelEconomy {
    
    @Id
    @Column(name = "fuel_id")
    private Long fuelId;
    
    @Column(name = "car_model_id", nullable = false)
    private Long carModelId;
    
    @Column(name = "fuel_type", length = 16)
    private String fuelType;
    
    @Column(name = "avg_fuel", precision = 5, scale = 2)
    private BigDecimal avgFuel;
    
    @Column(name = "sample_count")
    private Integer sampleCount;
    
    @Column(name = "collect_time")
    private LocalDate collectTime;
      /**
     * 关联的车型（一对一关系）
     * 通过car_model_id外键建立关联
     * 每个车型只有一个油耗经济性数据记录
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id", insertable = false, updatable = false)
    private CarModel carModel;
    
    /**
     * 获取油耗显示文本
     */
    public String getFuelDisplayText() {
        if (avgFuel == null) {
            return "油耗数据未知";
        }
        return String.format("%.2fL/100km", avgFuel);
    }
    
    /**
     * 获取燃料类型显示文本
     */
    public String getFuelTypeDisplayText() {
        if (fuelType == null || fuelType.trim().isEmpty()) {
            return "燃料类型未知";
        }
        return fuelType;
    }
    
    /**
     * 检查样本数量是否充足
     */
    public boolean hasSufficientSamples() {
        return sampleCount != null && sampleCount >= 10;
    }
    
    /**
     * 获取样本数量显示文本
     */
    public String getSampleCountText() {
        if (sampleCount == null) {
            return "样本数量未知";
        }
        return "基于" + sampleCount + "个样本";
    }
}
