package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.CarModelConfig;
import cn.com.undefined.abdap_backend.entity.CarModelConfigId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 车型配置数据访问层
 */
@Repository
public interface CarModelConfigRepository extends JpaRepository<CarModelConfig, CarModelConfigId> {
    
    // 根据车型ID查询所有配置
    List<CarModelConfig> findByCarModelId(Long carModelId);
    
    // 根据配置ID查询所有车型
    List<CarModelConfig> findByConfigId(Long configId);
    
    // 根据车型ID和启用状态查询配置
    List<CarModelConfig> findByCarModelIdAndEnabled(Long carModelId, Byte enabled);
    
    // 查询某车型的标配项目
    @Query("SELECT cmc FROM CarModelConfig cmc WHERE cmc.carModelId = :carModelId AND cmc.enabled = 1")
    List<CarModelConfig> findStandardConfigsByCarModelId(@Param("carModelId") Long carModelId);
    
    // 查询某车型的选装项目
    @Query("SELECT cmc FROM CarModelConfig cmc WHERE cmc.carModelId = :carModelId AND cmc.enabled = 2")
    List<CarModelConfig> findOptionalConfigsByCarModelId(@Param("carModelId") Long carModelId);
}
