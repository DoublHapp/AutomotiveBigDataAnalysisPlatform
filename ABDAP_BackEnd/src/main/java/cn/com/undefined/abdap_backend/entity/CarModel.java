package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_model_id")
    private Long carModelId;
    
    @Column(name = "model_name", length = 64, nullable = false)
    private String modelName;
    
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
    
    /**
     * 获取完整车型名称（品牌 + 车型）
     */
    public String getFullModelName() {
        if (brand != null) {
            return brand.getBrandName() + " " + modelName;
        }
        return modelName;
    }
    
    /**
     * 获取价格显示文本
     */
    public String getPriceDisplayText() {
        if (officialPrice == null) {
            return "价格待定";
        }
        return String.format("%.2f万元", officialPrice.divide(new BigDecimal("10000")));
    }
    
    /**
     * 获取续航里程显示文本
     */
    public String getRangeDisplayText() {
        if (rangeKm == null) {
            return "续航里程未知";
        }
        return rangeKm + "公里";
    }
}
