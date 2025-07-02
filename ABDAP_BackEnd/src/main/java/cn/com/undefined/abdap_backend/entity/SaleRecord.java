package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sale_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleRecord {
    
    @Id
    @Column(name = "sale_id")
    private Long saleId;
    
    @Column(name = "car_model_id")
    private Long carModelId;
    
    @Column(name = "region_id")
    private Long regionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id", insertable = false, updatable = false)
    private CarModel carModel;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", insertable = false, updatable = false)
    private Region region;
    
    @Column(name = "sale_month")
    private LocalDate saleMonth;
    
    @Column(name = "sale_count")
    private Integer saleCount;
    
    @Column(name = "sale_amount", precision = 12, scale = 2)
    private BigDecimal saleAmount;
    
}
