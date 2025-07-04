package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 配置项数据传输对象
 * 用于前端展示配置项数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigDTO {
    
    /**
     * 配置ID
     */
    private Long configId;
    
    /**
     * 配置名称
     */
    private String configName;
    
    /**
     * 配置类型
     */
    private String configType;
}
