package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.OpinionDTO;
import cn.com.undefined.abdap_backend.entity.Opinion;
import cn.com.undefined.abdap_backend.repository.OpinionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 口碑评价业务逻辑层
 */
@Service
@Transactional(readOnly = true)
public class OpinionService {
    
    @Autowired
    private OpinionRepository repository;
    
    /**
     * 查询所有口碑评价
     * 返回DTO格式用于前端展示
     */
    public List<OpinionDTO> getAllOpinions() {
        List<Opinion> opinions = repository.findAll();
        return opinions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将Entity转换为DTO
     */
    private OpinionDTO convertToDTO(Opinion opinion) {
        OpinionDTO dto = new OpinionDTO();
        dto.setOpinionId(opinion.getOpinionId());
        dto.setCarModelId(opinion.getCarModelId());
        
        // 获取车型信息
        if (opinion.getCarModel() != null) {
            dto.setModelName(opinion.getCarModel().getModelName());
            
            // 获取品牌名称
            if (opinion.getCarModel().getBrand() != null) {
                dto.setBrandName(opinion.getCarModel().getBrand().getBrandName());
            }
        }
        
        // 设置评分相关信息
        dto.setScore(opinion.getScore());
        
        return dto;
    }
}
