package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * 车型销量排行榜DTO
 * 用于前端展示车型销量排行榜数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CarModelSalesRankingDTO extends CarModelDTO {

    /**
     * 销量
     */
    private Long saleCount;

    /**
     * 销量增长率（百分比，例：0.15表示15%）
     */
    private BigDecimal saleGrowthRate;

    /**
     * 市场份额（百分比，例：0.12表示12%）
     */
    private BigDecimal marketShare;

    /**
     * 口碑总评分
     */
    private Double opinionScore;

    public CarModelSalesRankingDTO(CarModelDTO carModelDTO, Long saleCount, BigDecimal saleGrowthRate,
            BigDecimal marketShare, Double opinionScore) {
        super(carModelDTO);
        this.saleCount = saleCount;
        this.saleGrowthRate = saleGrowthRate;
        this.marketShare = marketShare;
        this.opinionScore = opinionScore;
    }
}