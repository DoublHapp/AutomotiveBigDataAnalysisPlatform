package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.CarModelDTO;
import cn.com.undefined.abdap_backend.dto.CarModelDetailDTO;
import cn.com.undefined.abdap_backend.repository.CarModelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

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
        return carModelRepository.findAllCarModelDTOs();
    }

    public List<CarModelDTO> getCarModelsByPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return carModelRepository.findAllCarModelDTOsByPage(pageable).getContent();
    }

    /**
     * 根据ID获取车型详情
     */
    public CarModelDTO getCarModelById(Long id) {
        return carModelRepository.findCarModelDTOById(id);
    }

    /**
     * 车型关键字模糊搜索
     */
    public List<CarModelDTO> searchCarModels(String keyword, int limit) {
        return carModelRepository.findTopByKeyword(keyword, PageRequest.of(0, limit));
    }

    /**
     * 获取所有level列表
     */
    public List<String> getAllLevels() {
        return carModelRepository.findAllLevels();
    }

    /**
     * 获取所有engine_type列表
     */
    public List<String> getAllEngineTypes() {
        return carModelRepository.findAllEngineTypes();
    }

    /**
     * 模糊匹配factory列表
     */
    public List<String> getFactorysByKeyword(String keyword) {
        return carModelRepository.findFactorysByKeyword(
                "all".equalsIgnoreCase(keyword) ? null : keyword);
    }

    /**
     * 根据车型ID获取车型详情DTO
     * 包含品牌、评分、销售记录等信息
     */
    public CarModelDetailDTO getCarModelDetailDTOById(Long carModelId) {
        return carModelRepository.findCarModelDetailDTOById(carModelId);
    }
}
