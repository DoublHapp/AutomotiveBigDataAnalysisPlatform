package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 地区数据传输对象
 * 用于前后端数据交互的纯数据载体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegionDTO {
    
    /**
     * 地区ID
     */
    private Long regionId;
    
    /**
     * 地区名称
     */
    private String regionName;
    
    /**
     * 父级地区名称
     */
    private String parentRegion;

    /**
     * 拷贝构造函数
     */
    public RegionDTO(RegionDTO regionDTO) {
        this.regionId = regionDTO.getRegionId();
        this.regionName = regionDTO.getRegionName();
        this.parentRegion = regionDTO.getParentRegion();
    }
}
