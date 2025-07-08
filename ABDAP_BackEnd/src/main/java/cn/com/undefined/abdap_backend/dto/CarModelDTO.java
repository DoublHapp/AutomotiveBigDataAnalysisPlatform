package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 车型响应DTO
 * 用于前端展示车型列表数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarModelDTO {

    /**
     * 车型ID
     */
    private Long carModelId;

    /**
     * 车型名称
     */
    private String modelName;

    /**
     * 车型全名
     */
    private String modelFullName;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 品牌名称（从Brand实体获取）
     */
    private String brandName;

    /**
     * 车型级别（A级、B级、C级等）
     */
    private String level;

    /**
     * 上市日期
     */
    private LocalDate launchDate;

    /**
     * 官方指导价（单位：元）
     */
    private BigDecimal officialPrice;

    private String engineType;

    /**
     * 座位数
     */
    private Integer seatNum;

    /**
     * 驱动类型（前驱、后驱、四驱等）
     */
    private String driveType;

    /**
     * 续航里程（公里）
     */
    private Integer rangeKm;

    /**
     * 车型图片URL
     */
    private String imageUrl;

    /**
     * 拷贝构造函数
     */
    public CarModelDTO(CarModelDTO other) {
        this.carModelId = other.carModelId;
        this.modelName = other.modelName;
        this.modelFullName = other.modelFullName;
        this.brandId = other.brandId;
        this.brandName = other.brandName;
        this.level = other.level;
        this.launchDate = other.launchDate;
        this.officialPrice = other.officialPrice;
        this.engineType = other.engineType;
        this.seatNum = other.seatNum;
        this.driveType = other.driveType;
        this.rangeKm = other.rangeKm;
        this.imageUrl = other.imageUrl;
    }
}
