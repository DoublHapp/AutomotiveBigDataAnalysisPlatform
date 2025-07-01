package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.entity.User;
import cn.com.undefined.abdap_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 * 处理用户相关的HTTP请求
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // 允许跨域访问
public class UserController {
    
    @Autowired
    private UserService userService;
      /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 创建用户对象
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setEmail(request.getEmail());
            user.setMobile(request.getMobile());
            
            // 角色处理：验证并设置角色
            String role = request.getRole();
            if (role == null || role.trim().isEmpty()) {
                // 如果未指定角色，默认设置为普通用户
                user.setRole("USER");
            } else {
                // 验证角色是否合法（可以根据业务需求调整）
                if (isValidRole(role)) {
                    user.setRole(role.toUpperCase()); // 统一转换为大写
                } else {
                    response.put("success", false);
                    response.put("message", "无效的用户角色：" + role);
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // 调用Service层创建用户
            User createdUser = userService.createUser(user);
            
            // 构建成功响应（不返回密码）
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("data", createUserResponse(createdUser));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            // 处理业务异常
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            // 处理系统异常
            response.put("success", false);
            response.put("message", "注册失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 调用Service层进行用户认证
            User user = userService.authenticateUser(request.getCredential(), request.getPassword());
            
            // 构建成功响应（不返回密码）
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("data", createUserResponse(user));
            
            // 在实际项目中，这里还应该生成JWT token或session
            // response.put("token", jwtService.generateToken(user));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            // 处理认证失败
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            
        } catch (Exception e) {
            // 处理系统异常
            response.put("success", false);
            response.put("message", "登录失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 创建用户响应对象（去除敏感信息）
     */
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("userId", user.getUserId());
        userResponse.put("username", user.getUsername());
        userResponse.put("email", user.getEmail());
        userResponse.put("mobile", user.getMobile());
        userResponse.put("role", user.getRole());
        userResponse.put("regTime", user.getRegTime());
        // 注意：不返回密码等敏感信息
        return userResponse;
    }
      /**
     * 注册请求DTO
     */
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String mobile;
        private String role; // 用户角色，可选字段
        
        // Getters and Setters
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getMobile() {
            return mobile;
        }
        
        public void setMobile(String mobile) {
            this.mobile = mobile;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
    }
    
    /**
     * 登录请求DTO
     */
    public static class LoginRequest {
        private String credential; // 用户名或邮箱
        private String password;
        
        // Getters and Setters
        public String getCredential() {
            return credential;
        }
        
        public void setCredential(String credential) {
            this.credential = credential;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    /**
     * 验证角色是否合法
     */
    private boolean isValidRole(String role) {
        if (role == null) {
            return false;
        }
        String upperRole = role.toUpperCase();
        // 定义允许的角色列表（可以根据业务需求调整）
        return "USER".equals(upperRole) || 
               "ADMIN".equals(upperRole) || 
               "MANAGER".equals(upperRole) ||
               "ANALYST".equals(upperRole);
    }
}
