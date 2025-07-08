package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * 地区销量排行榜DTO
 * 用于前端展示地区销量排行榜数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class RegionSalesRankingDTO extends RegionDTO {

    /**
     * 地区销量
     */
    private Long saleCount;

    /**
     * 地区销量增长率（百分比，例：0.15表示15%）
     */
    private BigDecimal saleGrowthRate;

    /**
     * 地区市场份额（百分比，例：0.12表示12%）
     */
    private BigDecimal marketShare;

    public RegionSalesRankingDTO(RegionDTO regionDTO, Long saleCount, BigDecimal saleGrowthRate,
            BigDecimal marketShare) {
        super(regionDTO);
        this.saleCount = saleCount;
        this.saleGrowthRate = saleGrowthRate;
        this.marketShare = marketShare;
    }
}