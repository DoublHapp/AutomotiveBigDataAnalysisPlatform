package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.ConfigDTO;
import cn.com.undefined.abdap_backend.entity.CarModelConfig;
import cn.com.undefined.abdap_backend.entity.Config;
import cn.com.undefined.abdap_backend.repository.CarModelConfigRepository;
import cn.com.undefined.abdap_backend.repository.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 配置业务逻辑层
 */
@Service
@Transactional(readOnly = true)
public class ConfigService {
    
    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private CarModelConfigRepository carModelConfigRepository;
    
    /**
     * 根据车型ID查找该车型拥有的所有配置
     * @param carModelId 车型ID
     * @return 配置DTO列表
     */
    public List<ConfigDTO> getConfigsByCarModelId(Long carModelId) {
        //先在carModelConfigRepository中查找该id有的config
        List<CarModelConfig> CarModelConfigs = carModelConfigRepository.findByCarModelId(carModelId);

        //提取configId列表
        List<Long> configIds = CarModelConfigs.stream()
                .map(CarModelConfig::getConfigId)
                .collect(Collectors.toList());
        //根据configId列表查询Config实体
        List<Config> configs = configRepository.findAllById(configIds);
        //将Config实体转换为ConfigDTO
        return configs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将Config实体转换为ConfigDTO
     * @param config 配置实体
     * @return 配置DTO
     */
    private ConfigDTO convertToDTO(Config config) {
        ConfigDTO dto = new ConfigDTO();
        dto.setConfigId(config.getConfigId());
        dto.setConfigName(config.getConfigName());
        dto.setConfigType(config.getConfigType());
        return dto;
    }
}
