package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.RecommendHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 推荐历史数据访问层
 */
@Repository
public interface RecommendHistoryRepository extends JpaRepository<RecommendHistory, Long> {
    
}
