package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.FuelEconomy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * 燃油经济性数据访问层
 */
@Repository
public interface FuelEconomyRepository extends JpaRepository<FuelEconomy, Long> {
    
    /**
     * 根据车型ID查询油耗数据（一对一关系）
     * @param carModelId 车型ID
     * @return 油耗数据
     */
    Optional<FuelEconomy> findByCarModelId(Long carModelId);
    
    /**
     * 根据燃料类型查询油耗数据
     * @param fuelType 燃料类型
     * @return 油耗数据列表
     */
    List<FuelEconomy> findByFuelType(String fuelType);
}
