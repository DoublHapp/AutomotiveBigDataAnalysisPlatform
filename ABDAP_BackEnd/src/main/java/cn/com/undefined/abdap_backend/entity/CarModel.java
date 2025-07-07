package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 汽车车型实体类
 * 对应数据库car_model表
 */
@Entity
@Table(name = "car_model")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_model_id")
    private Long carModelId;
    
    /**
     * 例如 “星愿 xxx版”
     * TODO:目前未在其他地方使用，仅做实体对应，如有需要再作调整
     */
    @Column(name = "model_full_name", length = 64, nullable = false)
    private String modelFullName;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;
    
    @Column(name = "level", length = 32)
    private String level;
    
    @Column(name = "launch_date")
    private LocalDate launchDate;
    
    @Column(name = "official_price", precision = 10, scale = 2)
    private BigDecimal officialPrice;
    
    @Column(name = "engine_type", length = 32)
    private String engineType;
    
    @Column(name = "seat_num")
    private Integer seatNum;
    
    @Column(name = "drive_type", length = 16)
    private String driveType;
    
    @Column(name = "range_km")
    private Integer rangeKm;
    
    @Column(name = "model_name", length = 64, nullable = false)
    private String modelName;

    @Column(name = "image_url", length = 255)
    private String imageUrl;
    
    // 建立与Brand实体的关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", insertable = false, updatable = false)
    private Brand brand;
    
    /**
     * 该车型的所有配置项（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "carModel", fetch = FetchType.LAZY)
    private List<CarModelConfig> carModelConfigs;
    
    /**
     * 该车型的所有销售记录（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "carModel", fetch = FetchType.LAZY)
    private List<SaleRecord> saleRecords;
    
    /**
     * 该车型的油耗经济性数据（一对一关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     * 每个车型只有一个综合的油耗数据记录
     */
    @OneToOne(mappedBy = "carModel", fetch = FetchType.LAZY)
    private FuelEconomy fuelEconomy;
      /**
     * 该车型的口碑评价记录（一对一关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     * 每个车型只有一个综合的口碑评价记录
     */
    @OneToOne(mappedBy = "carModel", fetch = FetchType.LAZY)
    private Opinion opinion;
    
    /**
     * 该车型的预测记录（一对多关系)
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     * 每个车型可以有多个预测记录（不同地区、不同时期）
     */
    @OneToMany(mappedBy = "carModel", fetch = FetchType.LAZY)
    private List<Prediction> predictions;
    
    /**
     * 以该车型为本品牌车型的竞品分析记录（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "carModel", fetch = FetchType.LAZY)
    private List<CompetitorAnalysis> competitorAnalyses;
    
    /**
     * 以该车型为竞品车型的竞品分析记录（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "competitorModel", fetch = FetchType.LAZY)
    private List<CompetitorAnalysis> asCompetitorAnalyses;
}
