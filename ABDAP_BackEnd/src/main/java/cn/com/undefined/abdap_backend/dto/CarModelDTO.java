package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
     * 品牌名称
     */
    private String brandName;
    
    /**
     * 车型类型（SUV、轿车、MPV等）
     */
    private String type;
    
    /**
     * 动力类型：electric(电动), fuel(燃油), hybrid(混动)
     */
    private String powerType;
    
    /**
     * 价格区间（格式：XX.XX-XX.XX万）
     */
    private String priceRange;
    
    /**
     * 是否有效/激活状态
     */
    private Boolean isActive;
}
