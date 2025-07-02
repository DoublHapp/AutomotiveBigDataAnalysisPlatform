package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.PurchaseSurvey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 购车调研数据访问层
 */
@Repository
public interface PurchaseSurveyRepository extends JpaRepository<PurchaseSurvey, Long> {
    
}
