package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 车型销量排行DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarModelRankingDTO {
    
    private Long carModelId;
    private String carModelName;
    private Long totalSales;
    private Integer ranking;
}
