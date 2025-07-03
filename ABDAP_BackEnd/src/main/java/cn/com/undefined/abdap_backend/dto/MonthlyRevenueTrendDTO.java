package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * 月度销售额趋势DTO
 */
@Data
@NoArgsConstructor
public class MonthlyRevenueTrendDTO {
    
    private Integer year;
    private Integer month;
    private String monthLabel;     // 格式化的月份标签，如"2024-07"
    private BigDecimal revenue;    // 销售额
    
    public MonthlyRevenueTrendDTO(Integer year, Integer month, BigDecimal revenue) {
        this.year = year;
        this.month = month;
        this.revenue = revenue;
        this.monthLabel = String.format("%d-%02d", year, month);
    }
}
