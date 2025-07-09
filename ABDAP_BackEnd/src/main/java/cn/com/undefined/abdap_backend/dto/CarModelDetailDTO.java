package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 车型详情DTO，包含各项评分
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CarModelDetailDTO extends CarModelDTO {

    /**
     * 动力评分
     */
    private Double powerScore;

    /**
     * 操控评分
     */
    private Double controlScore;

    /**
     * 舒适性评分
     */
    private Double comfortScore;

    /**
     * 外观评分
     */
    private Double appearanceScore;

    /**
     * 配置评分
     */
    private Double configScore;

    /**
     * 总体评分
     */
    private Double totalScore;

    /**
     * 销量
     */
    private Long saleCount;

    public CarModelDetailDTO(Long carModelId, String modelName, String modelFullName, Long brandId, String brandName,
            String level, java.time.LocalDate launchDate, java.math.BigDecimal officialPrice,
            String engineType, Integer seatNum, String driveType, Integer rangeKm, String imageUrl,
            Double powerScore, Double controlScore, Double comfortScore, Double appearanceScore,
            Double configScore, Double totalScore, Long saleCount) {
        super(carModelId, modelName, modelFullName, brandId, brandName, level, launchDate, officialPrice,
                engineType, seatNum, driveType, rangeKm, imageUrl);
        this.powerScore = powerScore;
        this.controlScore = controlScore;
        this.comfortScore = comfortScore;
        this.appearanceScore = appearanceScore;
        this.configScore = configScore;
        this.totalScore = totalScore;
        this.saleCount = saleCount;
    }
}