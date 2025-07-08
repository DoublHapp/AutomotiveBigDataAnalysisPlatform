package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.OpinionDTO;
import cn.com.undefined.abdap_backend.repository.OpinionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return repository.findAllOpinionDTOs();
    }
}
