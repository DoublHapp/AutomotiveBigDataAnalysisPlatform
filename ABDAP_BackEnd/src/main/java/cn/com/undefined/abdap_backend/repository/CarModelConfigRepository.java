package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.CarModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 车型配置数据访问层
 */
@Repository
public interface CarModelConfigRepository extends JpaRepository<CarModelConfig, Long> {
    
}
