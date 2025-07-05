package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.CarModelDTO;
import cn.com.undefined.abdap_backend.entity.CarModel;
import cn.com.undefined.abdap_backend.repository.CarModelRepository;
import jakarta.persistence.EntityNotFoundException;

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
        dto.setBrandId(carModel.getBrandId());
        // 安全设置品牌名称
        try {
            if (carModel.getBrand() != null) {
                dto.setBrandName(carModel.getBrand().getBrandName());
            } else {
                dto.setBrandName(null);
            }
        } catch (EntityNotFoundException e) {
            // 如果获取车型信息失败，设置为null
            dto.setBrandName(null);
        }

        // 设置车型级别
        dto.setLevel(carModel.getLevel());

        // 设置上市日期
        dto.setLaunchDate(carModel.getLaunchDate());

        // 设置官方指导价
        dto.setOfficialPrice(carModel.getOfficialPrice());

        // 设置发动机类型
        dto.setEngineType(carModel.getEngineType());

        // 设置座位数
        dto.setSeatNum(carModel.getSeatNum());

        // 设置驱动类型
        dto.setDriveType(carModel.getDriveType());

        // 设置续航里程
        dto.setRangeKm(carModel.getRangeKm());

        return dto;
    }
}
