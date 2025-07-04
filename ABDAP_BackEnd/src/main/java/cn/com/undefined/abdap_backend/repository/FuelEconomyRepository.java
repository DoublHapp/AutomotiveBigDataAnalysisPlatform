package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.FuelEconomy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 燃油经济性数据访问层
 */
@Repository
public interface FuelEconomyRepository extends JpaRepository<FuelEconomy, Long> {
    
    /**
     * 根据车型ID查询油耗数据
     * @param carModelId 车型ID
     * @return 油耗数据列表
     */
    List<FuelEconomy> findByCarModelCarModelId(Long carModelId);
    
    /**
     * 根据燃料类型查询油耗数据
     * @param fuelType 燃料类型
     * @return 油耗数据列表
     */
    List<FuelEconomy> findByFuelType(String fuelType);
    
    /**
     * 根据车型ID和燃料类型查询油耗数据
     * @param carModelId 车型ID
     * @param fuelType 燃料类型
     * @return 油耗数据列表
     */
    List<FuelEconomy> findByCarModelCarModelIdAndFuelType(Long carModelId, String fuelType);
}
