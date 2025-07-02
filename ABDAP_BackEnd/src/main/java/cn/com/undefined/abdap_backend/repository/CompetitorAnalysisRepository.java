package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.CompetitorAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 竞品分析数据访问层
 */
@Repository
public interface CompetitorAnalysisRepository extends JpaRepository<CompetitorAnalysis, Long> {
    
}
