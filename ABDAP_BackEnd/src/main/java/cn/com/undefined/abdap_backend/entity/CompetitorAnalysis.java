package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * 竞品分析实体类
 * 对应数据库competitor_analysis表
 */
@Entity
@Table(name = "competitor_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitorAnalysis {
    
    @Id
    @Column(name = "compare_id")
    private Long compareId;
    
    @Column(name = "car_model_id", nullable = false)
    private Long carModelId;
    
    @Column(name = "competitor_model_id", nullable = false)
    private Long competitorModelId;
    
    @Column(name = "compare_time")
    private LocalDateTime compareTime;
    
    @Column(name = "compare_content", columnDefinition = "TEXT")
    private String compareContent;
    
    @Column(name = "report_file", length = 256)
    private String reportFile;
}
