package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.ConfigDTO;
import cn.com.undefined.abdap_backend.service.ConfigService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 配置控制器
 */
@RestController
@RequestMapping("/api/configs")
@CrossOrigin(origins = "*")
public class ConfigController {
    
    @Autowired
    private ConfigService configService;
    
    /**
     * 根据车型ID查询该车型的所有配置
     * GET /api/configs?carModelId={carModelId}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConfigDTO>>> getConfigsByCarModelId(
            @RequestParam Long carModelId) {
        
        List<ConfigDTO> configs = configService.getConfigsByCarModelId(carModelId);
        return ResponseUtil.success(configs);
    }
}
