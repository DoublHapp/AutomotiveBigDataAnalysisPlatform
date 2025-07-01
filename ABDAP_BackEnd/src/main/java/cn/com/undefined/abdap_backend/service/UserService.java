package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.entity.User;
import cn.com.undefined.abdap_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户业务逻辑层
 */
@Service
@Transactional
public class UserService {
      @Autowired
    private UserRepository userRepository;
    
    // 注入密码编码器
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 获取所有用户
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    /**
     * 分页获取用户列表
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * 根据ID获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * 根据用户名获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 根据邮箱获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * 根据手机号获取用户
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByMobile(String mobile) {
        return userRepository.findByMobile(mobile);
    }
    
    /**
     * 根据用户名或邮箱获取用户（用于登录）
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsernameOrEmail(String credential) {
        return userRepository.findByUsernameOrEmail(credential);
    }
    
    /**
     * 根据角色查询用户
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }
    
    /**
     * 根据用户名模糊搜索
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByUsername(String username) {
        return userRepository.findByUsernameContainingIgnoreCase(username);
    }
    
    /**
     * 根据关键词搜索用户
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }
    
    /**
     * 多条件搜索用户
     */
    @Transactional(readOnly = true)
    public List<User> searchUsersByConditions(String username, String email, String mobile, String role) {
        return userRepository.findByMultipleConditions(username, email, mobile, role);
    }
    
    /**
     * 获取最近注册的用户
     */
    @Transactional(readOnly = true)
    public List<User> getRecentUsers() {
        return userRepository.findRecentUsers();
    }
    
    /**
     * 获取活跃用户（最近30天注册）
     */
    @Transactional(readOnly = true)
    public List<User> getActiveUsers() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return userRepository.findActiveUsers(thirtyDaysAgo);
    }
    
    /**
     * 根据注册时间范围查询用户
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByRegTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return userRepository.findByRegTimeBetween(startTime, endTime);
    }
    
    /**
     * 根据用户ID列表批量获取用户
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByIds(List<Long> userIds) {
        return userRepository.findByUserIds(userIds);
    }
    
    /**
     * 获取所有角色列表
     */
    @Transactional(readOnly = true)
    public List<String> getAllRoles() {
        return userRepository.findAllRoles();
    }
    
    /**
     * 创建新用户
     */
    public User createUser(User user) {
        // 验证用户信息
        validateUserForCreation(user);
          // 设置注册时间
        user.setRegTime(LocalDateTime.now());
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 设置默认角色
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole("USER");
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 更新用户信息
     */
    public User updateUser(Long id, User updatedUser) {
        Optional<User> existingUser = userRepository.findById(id);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // 验证更新信息
            validateUserForUpdate(user, updatedUser);
            
            // 更新字段（不包括密码和注册时间）
            user.setUsername(updatedUser.getUsername());
            user.setEmail(updatedUser.getEmail());
            user.setMobile(updatedUser.getMobile());
            user.setRole(updatedUser.getRole());
            
            return userRepository.save(user);
        }
        throw new RuntimeException("用户不存在，ID: " + id);
    }
    
    /**
     * 更新用户密码
     */
    public void updatePassword(Long id, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
              // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("原密码不正确");
            }
            
            // 更新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        } else {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
    }
    
    /**
     * 重置用户密码（管理员操作）
     */
    public void resetPassword(Long id, String newPassword) {
        Optional<User> userOpt = userRepository.findById(id);        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        } else {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在，ID: " + id);
        }
        userRepository.deleteById(id);
    }
    
    /**
     * 用户登录验证
     */
    @Transactional(readOnly = true)
    public User authenticateUser(String username, String password) {
          System.out.println("开始验证用户: " + username);
    
    // 先尝试根据用户名查找
    Optional<User> userOpt = userRepository.findByUsername(username);
    
    if (userOpt.isPresent()) {
        User user = userOpt.get();
        System.out.println("找到用户: " + user.getUsername() + ", 角色: " + user.getRole());
        
        // 验证密码
        if (passwordEncoder.matches(password, user.getPassword())) {
            System.out.println("密码验证成功");
            return user;
        } else {
            System.out.println("密码验证失败");
            throw new RuntimeException("密码错误");
        }
    } else {
        System.out.println("用户不存在: " + username);
        throw new RuntimeException("该账号不存在，请注册");
    }
 }
    
    /**
     * 检查用户名是否可用
     */
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
    
    /**
     * 检查邮箱是否可用
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
    
    /**
     * 检查手机号是否可用
     */
    @Transactional(readOnly = true)
    public boolean isMobileAvailable(String mobile) {
        return !userRepository.existsByMobile(mobile);
    }
    
    /**
     * 获取用户统计信息
     */
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        Long totalUsers = userRepository.countAllUsers();
        List<Object[]> roleStats = userRepository.countUsersByRole();
        List<User> activeUsers = getActiveUsers();
        
        return new UserStatistics(totalUsers, roleStats, activeUsers.size());
    }
    
    /**
     * 获取用户注册统计
     */
    @Transactional(readOnly = true)
    public List<Object[]> getRegistrationStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        return userRepository.findRegistrationStatistics(startDate, endDate);
    }
    
    /**
     * 批量创建用户
     */
    public List<User> createUsersInBatch(List<User> users) {
        // 验证所有用户
        for (User user : users) {
            validateUserForCreation(user);
            user.setRegTime(LocalDateTime.now());
            if (!StringUtils.hasText(user.getRole())) {
                user.setRole("USER");
            }
        }
        return userRepository.saveAll(users);
    }
    
    /**
     * 验证用户创建信息
     */
    private void validateUserForCreation(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new RuntimeException("密码不能为空");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在: " + user.getUsername());
        }
        if (StringUtils.hasText(user.getEmail()) && userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已存在: " + user.getEmail());
        }
        if (StringUtils.hasText(user.getMobile()) && userRepository.existsByMobile(user.getMobile())) {
            throw new RuntimeException("手机号已存在: " + user.getMobile());
        }
    }
    
    /**
     * 验证用户更新信息
     */
    private void validateUserForUpdate(User existingUser, User updatedUser) {
        if (!StringUtils.hasText(updatedUser.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        
        // 检查用户名是否被其他用户使用
        if (!existingUser.getUsername().equals(updatedUser.getUsername())) {
            if (userRepository.existsByUsername(updatedUser.getUsername())) {
                throw new RuntimeException("用户名已存在: " + updatedUser.getUsername());
            }
        }
        
        // 检查邮箱是否被其他用户使用
        if (StringUtils.hasText(updatedUser.getEmail()) && 
            !updatedUser.getEmail().equals(existingUser.getEmail())) {
            if (userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new RuntimeException("邮箱已存在: " + updatedUser.getEmail());
            }
        }
        
        // 检查手机号是否被其他用户使用
        if (StringUtils.hasText(updatedUser.getMobile()) && 
            !updatedUser.getMobile().equals(existingUser.getMobile())) {
            if (userRepository.existsByMobile(updatedUser.getMobile())) {
                throw new RuntimeException("手机号已存在: " + updatedUser.getMobile());
            }
        }
    }
    
    /**
     * 用户统计信息内部类
     */
    public static class UserStatistics {
        private Long totalUsers;
        private List<Object[]> roleStatistics;
        private Integer activeUsers;
        
        public UserStatistics(Long totalUsers, List<Object[]> roleStatistics, Integer activeUsers) {
            this.totalUsers = totalUsers;
            this.roleStatistics = roleStatistics;
            this.activeUsers = activeUsers;
        }
        
        // Getters
        public Long getTotalUsers() { return totalUsers; }
        public List<Object[]> getRoleStatistics() { return roleStatistics; }
        public Integer getActiveUsers() { return activeUsers; }
    }
}
