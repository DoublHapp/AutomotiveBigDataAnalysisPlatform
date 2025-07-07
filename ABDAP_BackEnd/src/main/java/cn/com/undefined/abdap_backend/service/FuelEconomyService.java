package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.FuelEconomyDTO;
import cn.com.undefined.abdap_backend.entity.FuelEconomy;
import cn.com.undefined.abdap_backend.repository.FuelEconomyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 燃油经济性数据业务逻辑层
 */
@Service
public class FuelEconomyService {

    @Autowired
    private FuelEconomyRepository fuelEconomyRepository;

    /**
     * 获取所有燃油经济性数据
     * 
     * @return 燃油经济性数据DTO列表
     */
    public List<FuelEconomyDTO> getAllFuelEconomy() {
        List<FuelEconomy> fuelEconomies = fuelEconomyRepository.findAll();
        return fuelEconomies.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据车型ID获取燃油经济性数据（一对一关系）
     * 
     * @param carModelId 车型ID
     * @return 燃油经济性数据DTO，如果不存在则返回null
     */
    public FuelEconomyDTO getFuelEconomyByCarModelId(Long carModelId) {
        return fuelEconomyRepository.findByCarModelId(carModelId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * 根据燃料类型获取燃油经济性数据
     * 
     * @param fuelType 燃料类型
     * @return 燃油经济性数据DTO列表
     */
    public List<FuelEconomyDTO> getFuelEconomyByFuelType(String fuelType) {
        List<FuelEconomy> fuelEconomies = fuelEconomyRepository.findByFuelType(fuelType);
        return fuelEconomies.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将FuelEconomy实体转换为FuelEconomyDTO
     * 
     * @param fuelEconomy 燃油经济性实体
     * @return 燃油经济性数据DTO
     */
    private FuelEconomyDTO convertToDTO(FuelEconomy fuelEconomy) {
        FuelEconomyDTO dto = new FuelEconomyDTO();
        dto.setFuelId(fuelEconomy.getFuelId());
        dto.setCarModelId(fuelEconomy.getCarModelId());
        dto.setFuelType(fuelEconomy.getFuelType());
        dto.setAvgFuel(fuelEconomy.getAvgFuel());
        dto.setSampleCount(fuelEconomy.getSampleCount());
        dto.setCollectTime(fuelEconomy.getCollectTime());
        return dto;
    }
}
