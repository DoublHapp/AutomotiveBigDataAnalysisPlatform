package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.CarModelDTO;
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
}
