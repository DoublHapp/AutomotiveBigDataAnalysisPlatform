package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 油耗经济性数据传输对象
 * 纯数据DTO，用于前端数据展示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuelEconomyDTO {
    
    /**
     * 油耗数据ID
     */
    private Long fuelId;
    
    /**
     * 关联的车型ID
     */
    private Long carModelId;
    
    /**
     * 燃料类型（如：汽油、柴油、混动、纯电动等）
     */
    private String fuelType;
    
    /**
     * 平均油耗（L/100km）
     */
    private BigDecimal avgFuel;
    
    /**
     * 样本数量
     */
    private Integer sampleCount;
    
    /**
     * 数据采集时间
     */
    private LocalDate collectTime;
}
