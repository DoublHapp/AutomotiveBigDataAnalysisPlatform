package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
}
