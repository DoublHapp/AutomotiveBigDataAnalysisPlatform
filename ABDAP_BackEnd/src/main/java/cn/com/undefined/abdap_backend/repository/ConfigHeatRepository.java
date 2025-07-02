package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.ConfigHeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 配置热力图数据访问层
 */
@Repository
public interface ConfigHeatRepository extends JpaRepository<ConfigHeat, Long> {
    
}
