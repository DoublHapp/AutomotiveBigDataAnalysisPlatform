package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 排行榜数据访问层
 */
@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {
    
}
