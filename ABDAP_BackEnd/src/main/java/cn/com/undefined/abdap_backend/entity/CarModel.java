package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", insertable = false, updatable = false)
    private Brand brand;
}
