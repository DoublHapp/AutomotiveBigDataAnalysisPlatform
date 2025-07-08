package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * 油耗排行榜DTO
 * 用于前端展示油耗排行榜数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class FuelConsumptionRankingDTO extends CarModelDTO {

    /**
     * 年油费预估（单位：元，按每年1.5万公里，每升油7.5元计算）
     */
    private BigDecimal annualFuelCost;

    /**
     * 口碑总评分
     */
    private BigDecimal opinionScore;

    public FuelConsumptionRankingDTO(CarModelDTO carModelDTO, BigDecimal annualFuelCost, BigDecimal opinionScore) {
        super(carModelDTO);
        this.annualFuelCost = annualFuelCost;
        this.opinionScore = opinionScore;
    }
}