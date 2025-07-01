package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查询用户（精确匹配）
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查询用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据手机号查询用户
     */
    Optional<User> findByMobile(String mobile);
    
    /**
     * 根据用户名模糊查询
     */
    List<User> findByUsernameContainingIgnoreCase(String username);
    
    /**
     * 根据角色查询用户
     */
    List<User> findByRole(String role);
    
    /**
     * 根据注册时间范围查询用户
     */
    List<User> findByRegTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询指定时间之后注册的用户
     */
    List<User> findByRegTimeAfter(LocalDateTime regTime);
    
    /**
     * 查询指定时间之前注册的用户
     */
    List<User> findByRegTimeBefore(LocalDateTime regTime);
    
    /**
     * 根据角色和注册时间查询用户
     */
    List<User> findByRoleAndRegTimeAfter(String role, LocalDateTime regTime);
    
    /**
     * 检查用户名是否已存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否已存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 检查手机号是否已存在
     */
    boolean existsByMobile(String mobile);
    
    /**
     * 根据用户名或邮箱查询用户（用于登录）
     */
    @Query("SELECT u FROM User u WHERE u.username = :credential OR u.email = :credential")
    Optional<User> findByUsernameOrEmail(@Param("credential") String credential);
    
    /**
     * 根据多个条件搜索用户
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:mobile IS NULL OR u.mobile LIKE CONCAT('%', :mobile, '%')) AND " +
           "(:role IS NULL OR u.role = :role) " +
           "ORDER BY u.regTime DESC")
    List<User> findByMultipleConditions(
        @Param("username") String username,
        @Param("email") String email,
        @Param("mobile") String mobile,
        @Param("role") String role
    );
    
    /**
     * 查询最近注册的用户
     */
    @Query("SELECT u FROM User u ORDER BY u.regTime DESC")
    List<User> findRecentUsers();
    
    /**
     * 查询活跃用户（最近30天注册）
     */
    @Query("SELECT u FROM User u WHERE u.regTime > :thirtyDaysAgo ORDER BY u.regTime DESC")
    List<User> findActiveUsers(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
    
    /**
     * 根据角色统计用户数量
     */
    @Query("SELECT u.role, COUNT(u) as userCount FROM User u " +
           "WHERE u.role IS NOT NULL " +
           "GROUP BY u.role " +
           "ORDER BY userCount DESC")
    List<Object[]> countUsersByRole();
    
    /**
     * 查询指定日期范围内的用户注册统计
     */
    @Query("SELECT DATE(u.regTime) as regDate, COUNT(u) as userCount " +
           "FROM User u " +
           "WHERE u.regTime BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(u.regTime) " +
           "ORDER BY regDate ASC")
    List<Object[]> findRegistrationStatistics(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 查询有邮箱的用户
     */
    @Query("SELECT u FROM User u WHERE u.email IS NOT NULL AND u.email != ''")
    List<User> findUsersWithEmail();
    
    /**
     * 查询有手机号的用户
     */
    @Query("SELECT u FROM User u WHERE u.mobile IS NOT NULL AND u.mobile != ''")
    List<User> findUsersWithMobile();
    
    /**
     * 根据用户ID列表批量查询用户
     */
    @Query("SELECT u FROM User u WHERE u.userId IN :userIds")
    List<User> findByUserIds(@Param("userIds") List<Long> userIds);
    
    /**
     * 查询所有角色列表（去重）
     */
    @Query("SELECT DISTINCT u.role FROM User u WHERE u.role IS NOT NULL ORDER BY u.role")
    List<String> findAllRoles();
    
    /**
     * 根据关键词搜索用户（用户名、邮箱、手机号）
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "u.mobile LIKE CONCAT('%', :keyword, '%')")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    /**
     * 查询用户总数
     */
    @Query("SELECT COUNT(u) FROM User u")
    Long countAllUsers();
    
    /**
     * 查询指定角色的用户总数
     */
    Long countByRole(String role);
}
