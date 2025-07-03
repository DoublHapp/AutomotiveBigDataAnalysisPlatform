package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度销量趋势DTO
 */
@Data
@NoArgsConstructor
public class MonthlySalesTrendDTO {
    
    private Integer year;
    private Integer month;
    private String monthLabel;  // 格式化的月份标签，如"2024-07"
    private Long salesCount;    // 销量
    
    public MonthlySalesTrendDTO(Integer year, Integer month, Long salesCount) {
        this.year = year;
        this.month = month;
        this.salesCount = salesCount;
        this.monthLabel = String.format("%d-%02d", year, month);
    }
}
