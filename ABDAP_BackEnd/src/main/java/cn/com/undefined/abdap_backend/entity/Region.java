package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Entity
@Table(name = "region")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Region {

    @Id
    @Column(name = "region_id")
    private Long regionId;
    @Column(name = "region_name", length = 64)
    private String regionName;

    @Column(name = "parent_region_id")
    private Long parentRegionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_region_id", insertable = false, updatable = false)
    private Region parentRegion;

    @OneToMany(mappedBy = "parentRegion", fetch = FetchType.LAZY)
    private List<Region> childRegions;
    
    /**
     * 该地区的所有销售记录（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
    private List<SaleRecord> saleRecords;
    
    /**
     * 该地区的预测记录（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     * 每个地区可以有多个预测记录（不同车型、不同时期）
     */
    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
    private List<Prediction> predictions;
}
