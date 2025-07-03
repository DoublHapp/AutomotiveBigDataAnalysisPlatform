package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.entity.CarModelConfig;
import cn.com.undefined.abdap_backend.entity.CarModelConfigId;
import cn.com.undefined.abdap_backend.repository.CarModelConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 车型配置业务逻辑层
 * 演示复合主键的各种数据库操作
 */
@Service
@Transactional
public class CarModelConfigService {

    @Autowired
    private CarModelConfigRepository repository;

    // ==================== 查询操作 ====================
    
    /**
     * 根据复合主键查询单条记录
     */
    public Optional<CarModelConfig> findById(Long carModelId, Long configId) {
        CarModelConfigId id = new CarModelConfigId(carModelId, configId);
        return repository.findById(id);
    }

    /**
     * 查询所有记录
     */
    public List<CarModelConfig> findAll() {
        return repository.findAll();
    }

    /**
     * 根据车型ID查询所有配置
     */
    public List<CarModelConfig> findByCarModelId(Long carModelId) {
        return repository.findByCarModelId(carModelId);
    }

    /**
     * 查询某车型的标配项目
     */
    public List<CarModelConfig> findStandardConfigs(Long carModelId) {
        return repository.findStandardConfigsByCarModelId(carModelId);
    }

    /**
     * 查询某车型的选装项目
     */
    public List<CarModelConfig> findOptionalConfigs(Long carModelId) {
        return repository.findOptionalConfigsByCarModelId(carModelId);
    }

    // ==================== 新增操作 ====================
    
    /**
     * 新增车型配置关系
     */
    public CarModelConfig save(Long carModelId, Long configId, Byte enabled, 
                              BigDecimal optionalPrice, Byte score) {
        CarModelConfig config = new CarModelConfig();
        config.setCarModelId(carModelId);
        config.setConfigId(configId);
        config.setEnabled(enabled);
        config.setOptionalPrice(optionalPrice);
        config.setScore(score);
        
        return repository.save(config);
    }

    /**
     * 批量新增配置
     */
    public List<CarModelConfig> saveAll(List<CarModelConfig> configs) {
        return repository.saveAll(configs);
    }

    // ==================== 修改操作 ====================
    
    /**
     * 更新配置状态
     */
    public boolean updateConfigStatus(Long carModelId, Long configId, Byte enabled) {
        Optional<CarModelConfig> optionalConfig = findById(carModelId, configId);
        if (optionalConfig.isPresent()) {
            CarModelConfig config = optionalConfig.get();
            config.setEnabled(enabled);
            repository.save(config);
            return true;
        }
        return false;
    }

    /**
     * 更新选装价格
     */
    public boolean updateOptionalPrice(Long carModelId, Long configId, BigDecimal price) {
        Optional<CarModelConfig> optionalConfig = findById(carModelId, configId);
        if (optionalConfig.isPresent()) {
            CarModelConfig config = optionalConfig.get();
            config.setOptionalPrice(price);
            repository.save(config);
            return true;
        }
        return false;
    }

    /**
     * 更新配置评分
     */
    public boolean updateScore(Long carModelId, Long configId, Byte score) {
        Optional<CarModelConfig> optionalConfig = findById(carModelId, configId);
        if (optionalConfig.isPresent()) {
            CarModelConfig config = optionalConfig.get();
            config.setScore(score);
            repository.save(config);
            return true;
        }
        return false;
    }

    // ==================== 删除操作 ====================
    
    /**
     * 根据复合主键删除记录
     */
    public boolean deleteById(Long carModelId, Long configId) {
        CarModelConfigId id = new CarModelConfigId(carModelId, configId);
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * 删除某车型的所有配置
     */
    public void deleteByCarModelId(Long carModelId) {
        List<CarModelConfig> configs = repository.findByCarModelId(carModelId);
        repository.deleteAll(configs);
    }

    /**
     * 删除某配置的所有关联
     */
    public void deleteByConfigId(Long configId) {
        List<CarModelConfig> configs = repository.findByConfigId(configId);
        repository.deleteAll(configs);
    }

    // ==================== 存在性检查 ====================
    
    /**
     * 检查记录是否存在
     */
    public boolean exists(Long carModelId, Long configId) {
        CarModelConfigId id = new CarModelConfigId(carModelId, configId);
        return repository.existsById(id);
    }

    /**
     * 统计记录数量
     */
    public long count() {
        return repository.count();
    }

    /**
     * 统计某车型的配置数量
     */
    public long countByCarModelId(Long carModelId) {
        return repository.findByCarModelId(carModelId).size();
    }
}
