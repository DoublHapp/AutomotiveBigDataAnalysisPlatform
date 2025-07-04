package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 车型数据访问层
 */
@Repository
public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    
}
