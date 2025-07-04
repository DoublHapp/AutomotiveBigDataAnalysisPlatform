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
}
