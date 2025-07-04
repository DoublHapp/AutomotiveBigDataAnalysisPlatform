package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.OpinionDTO;
import cn.com.undefined.abdap_backend.service.OpinionService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 口碑评价控制器
 */
@RestController
@RequestMapping("/api/opinions")
@CrossOrigin(origins = "*")
public class OpinionController {
    
    @Autowired
    private OpinionService service;
    
    /**
     * 查询所有口碑评价
     * GET /api/opinions
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OpinionDTO>>> getAllOpinions() {
        List<OpinionDTO> opinions = service.getAllOpinions();
        return ResponseUtil.success(opinions);
    }
}
