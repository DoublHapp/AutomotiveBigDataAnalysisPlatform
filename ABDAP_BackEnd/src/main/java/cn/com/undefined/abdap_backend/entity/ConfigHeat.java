package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "config_heat")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigHeat {
    
    @Id
    @Column(name = "heat_id")
    private Long heatId;
    
    @Column(name = "config_id")
    private Long configId;
    
    @Column(name = "collect_time")
    private LocalDate collectTime;
    
    @Column(name = "select_count")
    private Integer selectCount;
    
    /**
     * 关联的配置项（多对一关系）
     * 通过config_id外键建立关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", insertable = false, updatable = false)
    private Config config;
    
}
