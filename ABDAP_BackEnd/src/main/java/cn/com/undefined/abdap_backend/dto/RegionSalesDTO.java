package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 地区销量分布DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionSalesDTO {
    
    private Long regionId;
    private String regionName;
    private Long totalSales;
}
