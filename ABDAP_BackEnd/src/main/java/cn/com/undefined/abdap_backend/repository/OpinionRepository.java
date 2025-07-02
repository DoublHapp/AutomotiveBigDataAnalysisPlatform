package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.Opinion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 口碑评价数据访问层
 */
@Repository
public interface OpinionRepository extends JpaRepository<Opinion, Long> {
    
}
