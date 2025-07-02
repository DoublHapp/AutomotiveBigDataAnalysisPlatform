package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.FuelEconomy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 燃油经济性数据访问层
 */
@Repository
public interface FuelEconomyRepository extends JpaRepository<FuelEconomy, Long> {
    
}
