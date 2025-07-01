package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_survey")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseSurvey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_id")
    private Long surveyId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "fill_time")
    private LocalDateTime fillTime;
    
    @Column(name = "brand_pref", length = 256)
    private String brandPref;
    
    @Column(name = "daily_mileage", length = 32)
    private String dailyMileage;
    
    @Column(name = "seat_num", length = 16)
    private String seatNum;
    
    @Column(name = "energy_pref", length = 32)
    private String energyPref;
    
    @Column(name = "buy_priority", length = 64)
    private String buyPriority;
    
    @Column(name = "budget_range", length = 32)
    private String budgetRange;
    
    @Column(name = "appearance_pref", length = 128)
    private String appearancePref;
    
    @Column(name = "drive_type", length = 16)
    private String driveType;
    
    // 建立与User实体的关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
}
