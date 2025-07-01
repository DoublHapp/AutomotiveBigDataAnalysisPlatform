package cn.com.undefined.abdap_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

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
    
}
