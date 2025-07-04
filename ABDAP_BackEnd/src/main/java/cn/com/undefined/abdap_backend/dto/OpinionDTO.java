package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

/**
 * 口碑评价响应DTO
 * 用于前端展示口碑评价数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpinionDTO {
    
    /**
     * 口碑ID
     */
    private Long opinionId;
    
    /**
     * 车型ID
     */
    private Long carModelId;
    
    /**
     * 车型名称（从CarModel实体获取）
     */
    private String modelName;
    
    /**
     * 品牌名称（从CarModel.Brand实体获取）
     */
    private String brandName;
    
    /**
     * 评分
     */
    private BigDecimal score;
    
    /**
     * 评分显示文本（格式化后的评分，如：4.5分）
     */
    private String scoreDisplayText;
    
    /**
     * 评分等级（优秀、良好、一般、较差、很差）
     */
    private String scoreLevel;
    
    /**
     * 是否为高分评价
     */
    private Boolean isHighScore;
    
    /**
     * 是否为低分评价
     */
    private Boolean isLowScore;
}
