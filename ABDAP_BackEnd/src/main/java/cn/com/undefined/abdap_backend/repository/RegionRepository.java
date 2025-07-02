package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 区域数据访问层
 */
@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    
}
