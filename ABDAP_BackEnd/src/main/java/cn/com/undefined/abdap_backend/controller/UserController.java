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
            // user.setEmail(request.getEmail());
            // user.setMobile(request.getMobile());
            
            // 角色处理：验证并设置角色
            String role = request.getRole();
            if (role == null || role.trim().isEmpty()) {
                // 如果未指定角色，默认设置为普通用户
                user.setRole("Customer");
            } else {
                // 验证角色是否合法
                if (isValidRole(role)) {
                    user.setRole(role); 
                } else {
                    response.put("status", 0);
                    response.put("msg", "无效的用户角色：" + role);
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // 调用Service层创建用户
            User createdUser = userService.createUser(user);
            
            // 构建成功响应（不返回密码）
            response.put("status", 1);
            response.put("msg", "注册成功");
            response.put("data", createUserResponse(createdUser));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            // 处理业务异常(用户名已存在等)
            response.put("status", 0);
            response.put("msg", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            // 处理系统异常
            response.put("status",0);
            response.put("msg", "注册失败，请稍后重试");
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
            // 添加调试日志
            System.out.println("收到登录请求: " + request.getUsername());

            // 调用Service层进行用户认证
            User user = userService.authenticateUser(request.getUsername(), request.getPassword());
            
            // 构建成功响应（不返回密码）
            response.put("status", 1);
            response.put("msg", "登录成功");
    
            
            // 在实际项目中，这里还应该生成JWT token或session
            // response.put("token", jwtService.generateToken(user));

             // 构建用户数据，包含权限信息
            Map<String, Object> userData = createUserResponse(user);
            // 添加权限列表
            userData.put("permissions", getRolePermissions(user.getRole()));
            
            response.put("data", userData);

            System.out.println("登录成功，返回数据: " + response);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            // 处理认证失败
            System.out.println("登录失败: " + e.getMessage());
            response.put("status",0);
            response.put("msg", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            
        } catch (Exception e) {
            // 处理系统异常
            System.out.println("登录异常: " + e.getMessage());
            response.put("status",0);
            response.put("msg", "登录失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 创建用户响应对象（去除敏感信息）
     */
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("user_id", user.getUserId());
        userResponse.put("username", user.getUsername());
        // userResponse.put("email", user.getEmail());
        // userResponse.put("mobile", user.getMobile());
        userResponse.put("role", user.getRole());
        userResponse.put("regTime", user.getRegTime());
        // 注意：不返回密码等敏感信息
        return userResponse;
    }


      /**
     * 根据角色获取权限列表
     */
    private String[] getRolePermissions(String role) {
        switch (role) {
            case "SalesManager":
                return new String[]{"SaleTotal:view", "TopCarModelList:view", "SalesForecast:view"};
            case "Customer":
                return new String[]{"TopCarModelList:view", "Recommendation:view", "CarPurchasesHeatMap:view"};
            case "ProductManager":
                return new String[]{"VehicleConfiguration:view", "VehicleModelCompAnalysis:view", "FuelConsList:view"};
            default:
                return new String[]{"TopCarModelList:view"};
        }
    }
    



      /**
     * 注册请求DTO
     */
    public static class RegisterRequest {
        private String username;
        private String password;
        // private String email;
        // private String mobile;
        private String role; // 用户角色，SalesManager, Customer, ProductManager
        
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
        
        // public String getEmail() {
        //     return email;
        // }
        
        // public void setEmail(String email) {
        //     this.email = email;
        // }
        
        // public String getMobile() {
        //     return mobile;
        // }
        
        // public void setMobile(String mobile) {
        //     this.mobile = mobile;
        // }
        
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
        private String username; 
        private String password;
        
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
    }
    
    /**
     * 验证角色是否合法
     */
    private boolean isValidRole(String role) {
        if (role == null) {
            return false;
        }
        // 定义允许的角色列表
        return "SalesManager".equals(role) || 
               "Customer".equals(role) || 
               "ProductManager".equals(role);
    }
}
