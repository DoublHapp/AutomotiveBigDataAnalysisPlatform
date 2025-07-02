package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 汽车品牌实体类
 * 对应数据库brand表
 */
@Entity
@Table(name = "brand")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Brand {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    private Long brandId;
    
    @Column(name = "brand_name", length = 64, nullable = false)
    private String brandName;
    
    @Column(name = "factory", length = 64)
    private String factory;
    
    @Column(name = "brand_logo", length = 256)
    private String brandLogo;
    
    /**
     * 该品牌下的所有车型（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
    private List<CarModel> carModels;
    
    /**
     * 获取品牌完整信息（品牌名称 - 厂商）
     */
    public String getFullBrandInfo() {
        if (factory != null && !factory.trim().isEmpty()) {
            return brandName + " - " + factory;
        }
        return brandName;
    }
    
    /**
     * 检查是否有品牌Logo
     */
    public boolean hasLogo() {
        return brandLogo != null && !brandLogo.trim().isEmpty();
    }
}
