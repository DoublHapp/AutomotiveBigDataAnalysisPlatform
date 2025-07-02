package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.CarModelDTO;
import cn.com.undefined.abdap_backend.entity.CarModel;
import cn.com.undefined.abdap_backend.repository.CarModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 车型服务类
 * 提供车型相关的业务逻辑
 */
@Service
public class CarModelService {
    
    @Autowired
    private CarModelRepository carModelRepository;
    
    /**
     * 获取所有车型列表
     * 返回DTO格式用于前端展示
     */
    public List<CarModelDTO> getAllCarModels() {
        List<CarModel> carModels = carModelRepository.findAll();
        return carModels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID获取车型详情
     */
    public CarModelDTO getCarModelById(Long id) {
        CarModel carModel = carModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("车型不存在"));
        return convertToDTO(carModel);
    }
    
    /**
     * 将Entity转换为DTO
     */
    private CarModelDTO convertToDTO(CarModel carModel) {
        CarModelDTO dto = new CarModelDTO();
        dto.setCarModelId(carModel.getCarModelId());
        dto.setModelName(carModel.getModelName());
        
        // 获取品牌名称
        if (carModel.getBrand() != null) {
            dto.setBrandName(carModel.getBrand().getBrandName());
        }
        
        // 设置车型类型和动力类型
        dto.setPowerType(carModel.getEngineType()); // 发动机类型作为动力类型
        
        // 生成价格区间
        if (carModel.getOfficialPrice() != null) {
            dto.setPriceRange(generatePriceRange(carModel.getOfficialPrice()));
        }
        
        // 设置激活状态（根据上市日期判断）
        dto.setIsActive(carModel.getLaunchDate() != null);
        
        return dto;
    }
    
    /**
     * 生成价格区间字符串
     * 简化处理：以官方价格为基础生成一个区间
     */
    private String generatePriceRange(java.math.BigDecimal officialPrice) {
        if (officialPrice == null) {
            return "价格待定";
        }
        
        // 简单处理：生成±10%的价格区间
        java.math.BigDecimal lowerPrice = officialPrice.multiply(new java.math.BigDecimal("0.9"));
        java.math.BigDecimal upperPrice = officialPrice.multiply(new java.math.BigDecimal("1.1"));
        
        return String.format("%.2f-%.2f万", lowerPrice, upperPrice);
    }
}
