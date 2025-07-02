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
    
    /**
     * 关联的本品牌车型（多对一关系）
     * 通过car_model_id外键建立关联
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_model_id", insertable = false, updatable = false)
    private CarModel carModel;
    
    /**
     * 关联的竞品车型（多对一关系）
     * 通过competitor_model_id外键建立关联
     * 注意：这里使用不同的字段名来区分本品牌车型和竞品车型
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_model_id", insertable = false, updatable = false)
    private CarModel competitorModel;
    
    /**
     * 获取对比分析摘要（截取前200个字符）
     */
    public String getCompareContentSummary() {
        if (compareContent == null || compareContent.trim().isEmpty()) {
            return "暂无对比内容";
        }
        if (compareContent.length() <= 200) {
            return compareContent;
        }
        return compareContent.substring(0, 200) + "...";
    }
    
    /**
     * 检查是否有报告文件
     */
    public boolean hasReportFile() {
        return reportFile != null && !reportFile.trim().isEmpty();
    }
    
    /**
     * 获取报告文件名（不包含路径）
     */
    public String getReportFileName() {
        if (!hasReportFile()) {
            return "无报告文件";
        }
        
        // 提取文件名部分
        String fileName = reportFile;
        int lastSlashIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (lastSlashIndex >= 0 && lastSlashIndex < fileName.length() - 1) {
            fileName = fileName.substring(lastSlashIndex + 1);
        }
        return fileName;
    }
    
    /**
     * 获取对比分析的完整描述
     */
    public String getCompareDescription() {
        StringBuilder description = new StringBuilder();
        
        if (carModel != null && carModel.getModelName() != null) {
            description.append(carModel.getModelName());
        } else {
            description.append("车型ID:").append(carModelId);
        }
        
        description.append(" vs ");
        
        if (competitorModel != null && competitorModel.getModelName() != null) {
            description.append(competitorModel.getModelName());
        } else {
            description.append("竞品ID:").append(competitorModelId);
        }
        
        if (compareTime != null) {
            description.append(" (").append(compareTime.toLocalDate()).append(")");
        }
        
        return description.toString();
    }
    
    /**
     * 检查是否为同品牌对比
     */
    public boolean isSameBrandComparison() {
        return carModel != null && competitorModel != null 
            && carModel.getBrandId() != null && competitorModel.getBrandId() != null
            && carModel.getBrandId().equals(competitorModel.getBrandId());
    }
    
    /**
     * 检查对比内容是否充实
     */
    public boolean hasSubstantialContent() {
        return compareContent != null && compareContent.trim().length() >= 100;
    }
}
