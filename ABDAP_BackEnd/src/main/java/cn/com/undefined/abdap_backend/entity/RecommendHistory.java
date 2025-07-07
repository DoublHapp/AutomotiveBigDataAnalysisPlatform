package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "recommend_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommend_id")
    private Long recommendId;
    
    @Column(name = "survey_id", nullable = false)
    private Long surveyId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "recommend_time")
    private LocalDateTime recommendTime;
    
    @Column(name = "car_model_ids", length = 128)
    private String carModelIds;
    
    @Column(name = "enumtEditTime")
    private LocalDate enumtEditTime;
    
    @Column(name = "enumtIsDel")
    private Short enumtIsDel;
}
