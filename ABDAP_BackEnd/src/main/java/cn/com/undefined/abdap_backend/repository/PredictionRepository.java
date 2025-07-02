package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 预测数据访问层
 */
@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    
}
