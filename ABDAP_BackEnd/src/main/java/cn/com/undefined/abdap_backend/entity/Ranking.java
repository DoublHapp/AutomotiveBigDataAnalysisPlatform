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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rankingId;
    
    @Column(name = "rank_type", length = 32)
    private String rankType;
    
    @Column(name = "rank_time")
    private LocalDate rankTime;
    
    @Column(name = "ranking_data", columnDefinition = "LONGTEXT")
    private String rankingData;
}
