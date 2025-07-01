package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 配置项实体类
 * 对应数据库config表
 */
@Entity
@Table(name = "config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Config {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;
    
    @Column(name = "config_name", length = 64, nullable = false)
    private String configName;
    
    @Column(name = "config_type", length = 32)
    private String configType;
    
    /**
     * 检查配置名称是否有效
     */
    public boolean hasValidName() {
        return configName != null && !configName.trim().isEmpty();
    }
    
    /**
     * 获取配置显示信息
     */
    public String getConfigDisplayInfo() {
        if (configType != null && !configType.trim().isEmpty()) {
            return configName + " (" + configType + ")";
        }
        return configName;
    }
}
