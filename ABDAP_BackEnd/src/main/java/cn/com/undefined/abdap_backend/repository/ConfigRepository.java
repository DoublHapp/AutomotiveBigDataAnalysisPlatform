package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 配置数据访问层
 */
@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {
    
}
