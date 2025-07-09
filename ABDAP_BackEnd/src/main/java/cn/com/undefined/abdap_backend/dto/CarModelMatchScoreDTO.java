package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * 车型匹配度打分DTO
 * 用于前端展示购车推荐榜单的匹配度分数及各维度子分数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CarModelMatchScoreDTO extends CarModelDTO {

    /**
     * 匹配度总分（满分100）
     */
    private BigDecimal totalScore;

    /**
     * 预算匹配分（30%）
     */
    private BigDecimal budgetScore;

    /**
     * 车型类别匹配分（25%）
     */
    private BigDecimal levelScore;

    /**
     * 能源类型匹配分（20%）
     */
    private BigDecimal engineTypeScore;

    /**
     * 座位数匹配分（15%）
     */
    private BigDecimal seatNumScore;

    /**
     * 品牌偏好匹配分（10%）
     */
    private BigDecimal brandScore;

    public CarModelMatchScoreDTO(CarModelDTO carModelDTO, BigDecimal totalScore,
                                 BigDecimal budgetScore, BigDecimal levelScore,
                                 BigDecimal engineTypeScore, BigDecimal seatNumScore,
                                 BigDecimal brandScore) {
        super(carModelDTO);
        this.totalScore = totalScore;
        this.budgetScore = budgetScore;
        this.levelScore = levelScore;
        this.engineTypeScore = engineTypeScore;
        this.seatNumScore = seatNumScore;
        this.brandScore = brandScore;
    }
}