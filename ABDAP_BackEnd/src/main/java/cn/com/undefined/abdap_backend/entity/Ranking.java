package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

/**
 * 榜单实体类
 * 对应数据库ranking表
 */
@Entity
@Table(name = "ranking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ranking {
    
    @Id
    @Column(name = "ranking_id")
    private Long rankingId;
    
    @Column(name = "rank_type", length = 32)
    private String rankType;
    
    @Column(name = "rank_time")
    private LocalDate rankTime;
    
    @Column(name = "ranking_data", columnDefinition = "LONGTEXT")
    private String rankingData;
    
    /**
     * 获取榜单类型显示文本
     */
    public String getRankTypeDisplayText() {
        if (rankType == null || rankType.trim().isEmpty()) {
            return "未知榜单类型";
        }
        
        switch (rankType.toLowerCase()) {
            case "sales":
                return "销量榜";
            case "popularity":
                return "人气榜";
            case "price":
                return "价格榜";
            case "satisfaction":
                return "满意度榜";
            case "fuel_economy":
                return "节油榜";
            case "safety":
                return "安全性榜";
            case "brand":
                return "品牌榜";
            case "suv":
                return "SUV榜";
            case "sedan":
                return "轿车榜";
            case "mpv":
                return "MPV榜";
            case "luxury":
                return "豪华车榜";
            case "compact":
                return "紧凑型车榜";
            default:
                return rankType + "榜";
        }
    }
    
    /**
     * 获取榜单时间显示文本
     */
    public String getRankTimeDisplayText() {
        if (rankTime == null) {
            return "时间未知";
        }
        return rankTime.toString();
    }
    
    /**
     * 获取榜单数据摘要（截取前500个字符）
     */
    public String getRankingDataSummary() {
        if (rankingData == null || rankingData.trim().isEmpty()) {
            return "暂无榜单数据";
        }
        if (rankingData.length() <= 500) {
            return rankingData;
        }
        return rankingData.substring(0, 500) + "...";
    }
    
    /**
     * 检查是否有榜单数据
     */
    public boolean hasRankingData() {
        return rankingData != null && !rankingData.trim().isEmpty();
    }
    
    /**
     * 检查榜单数据是否为JSON格式
     */
    public boolean isJsonFormat() {
        if (!hasRankingData()) {
            return false;
        }
        String trimmedData = rankingData.trim();
        return (trimmedData.startsWith("{") && trimmedData.endsWith("}")) ||
               (trimmedData.startsWith("[") && trimmedData.endsWith("]"));
    }
    
    /**
     * 获取榜单完整描述
     */
    public String getRankingDescription() {
        StringBuilder description = new StringBuilder();
        description.append(getRankTypeDisplayText());
        
        if (rankTime != null) {
            description.append("（").append(rankTime).append("）");
        }
        
        return description.toString();
    }
    
    /**
     * 检查是否为当月榜单
     */
    public boolean isCurrentMonthRanking() {
        if (rankTime == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return rankTime.getYear() == now.getYear() && 
               rankTime.getMonth() == now.getMonth();
    }
    
    /**
     * 检查是否为本年度榜单
     */
    public boolean isCurrentYearRanking() {
        if (rankTime == null) {
            return false;
        }
        return rankTime.getYear() == LocalDate.now().getYear();
    }
    
    /**
     * 检查榜单是否过期（超过一年）
     */
    public boolean isExpired() {
        if (rankTime == null) {
            return true;
        }
        return rankTime.isBefore(LocalDate.now().minusYears(1));
    }
}
