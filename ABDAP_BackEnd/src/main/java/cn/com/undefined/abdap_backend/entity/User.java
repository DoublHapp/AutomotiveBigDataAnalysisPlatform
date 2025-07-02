package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "username", length = 64, nullable = false)
    private String username;
    
    @Column(name = "password", length = 128, nullable = false)
    private String password;
    
    @Column(name = "mobile", length = 20)
    private String mobile;
    
    @Column(name = "email", length = 64)
    private String email;
    
    @Column(name = "reg_time")
    private LocalDateTime regTime;
    
    @Column(name = "role", length = 64)
    private String role;
    
    /**
     * 该用户填写的所有购车调研表（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<PurchaseSurvey> purchaseSurveys;
    
    /**
     * 该用户的所有推荐记录（一对多关系）
     * 使用mappedBy属性建立双向关联，避免生成中间表
     * 懒加载，只有在访问时才从数据库查询
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<RecommendHistory> recommendHistories;
    
}
