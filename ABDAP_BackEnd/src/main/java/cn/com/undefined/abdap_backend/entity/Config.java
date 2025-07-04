package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

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
     * 该配置项在各个车型中的配置情况（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "config", fetch = FetchType.LAZY)
    private List<CarModelConfig> carModelConfigs;
    
    /**
     * 该配置项的热度统计记录（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "config", fetch = FetchType.LAZY)
    private List<ConfigHeat> configHeats;
}
