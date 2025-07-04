package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售记录数据传输对象
 * 用于前后端数据交互的纯数据载体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRecordDTO {
    
    /**
     * 销售记录ID
     */
    private Long saleId;
    
    /**
     * 车型ID
     */
    private Long carModelId;
    
    /**
     * 车型名称（冗余字段，便于前端显示）
     */
    private String carModelName;
    
    /**
     * 地区ID
     */
    private Long regionId;
    
    /**
     * 地区名称（冗余字段，便于前端显示）
     */
    private String regionName;
    
    /**
     * 销售月份
     */
    private LocalDate saleMonth;
    
    /**
     * 销售数量
     */
    private Integer saleCount;
    
    /**
     * 销售金额
     */
    private BigDecimal saleAmount;
}
