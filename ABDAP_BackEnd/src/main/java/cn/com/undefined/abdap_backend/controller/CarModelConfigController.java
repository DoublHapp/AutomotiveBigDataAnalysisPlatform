package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.entity.CarModelConfig;
import cn.com.undefined.abdap_backend.service.CarModelConfigService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 车型配置控制器
 * 演示复合主键的REST API操作
 */
@RestController
@RequestMapping("/api/car-model-configs")
@CrossOrigin(origins = "*")
public class CarModelConfigController {

    @Autowired
    private CarModelConfigService service;

    // ==================== 查询操作 ====================
      /**
     * 根据复合主键查询单条记录
     * GET /api/car-model-configs/{carModelId}/{configId}
     */
    @GetMapping("/{carModelId}/{configId}")
    public ResponseEntity<ApiResponse<CarModelConfig>> getById(@PathVariable Long carModelId, 
                                              @PathVariable Long configId) {
        Optional<CarModelConfig> config = service.findById(carModelId, configId);
        if (config.isPresent()) {
            return ResponseUtil.success(config.get());
        } else {
            return ResponseUtil.notFound("车型配置不存在");
        }
    }

    /**
     * 查询某车型的所有配置
     * GET /api/car-model-configs/car-model/{carModelId}
     */
    @GetMapping("/car-model/{carModelId}")
    public ResponseEntity<ApiResponse<List<CarModelConfig>>> getByCarModelId(@PathVariable Long carModelId) {
        List<CarModelConfig> configs = service.findByCarModelId(carModelId);
        return ResponseUtil.success(configs);
    }

    /**
     * 查询某车型的标配项目
     * GET /api/car-model-configs/car-model/{carModelId}/standard
     */
    @GetMapping("/car-model/{carModelId}/standard")
    public ResponseEntity<ApiResponse<List<CarModelConfig>>> getStandardConfigs(@PathVariable Long carModelId) {
        List<CarModelConfig> configs = service.findStandardConfigs(carModelId);
        return ResponseUtil.success(configs);
    }

    /**
     * 查询某车型的选装项目
     * GET /api/car-model-configs/car-model/{carModelId}/optional
     */
    @GetMapping("/car-model/{carModelId}/optional")
    public ResponseEntity<ApiResponse<List<CarModelConfig>>> getOptionalConfigs(@PathVariable Long carModelId) {
        List<CarModelConfig> configs = service.findOptionalConfigs(carModelId);
        return ResponseUtil.success(configs);
    }

    // ==================== 新增操作 ====================
      /**
     * 新增车型配置关系
     * POST /api/car-model-configs
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CarModelConfig>> create(@RequestBody CreateConfigRequest request) {
        // 检查是否已存在
        if (service.exists(request.getCarModelId(), request.getConfigId())) {
            return ResponseUtil.badRequest("该车型配置关系已存在");
        }

        CarModelConfig config = service.save(
            request.getCarModelId(),
            request.getConfigId(),
            request.getEnabled(),
            request.getOptionalPrice(),
            request.getScore()
        );
        
        return ResponseUtil.created(config);
    }

    // ==================== 修改操作 ====================
    
    /**
     * 更新配置状态
     * PUT /api/car-model-configs/{carModelId}/{configId}/status
     */
    @PutMapping("/{carModelId}/{configId}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(@PathVariable Long carModelId,
                                          @PathVariable Long configId,
                                          @RequestParam Byte enabled) {
        boolean updated = service.updateConfigStatus(carModelId, configId, enabled);
        if (updated) {
            return ResponseUtil.success("配置状态更新成功");
        } else {
            return ResponseUtil.notFound("车型配置不存在");
        }
    }

    /**
     * 更新选装价格
     * PUT /api/car-model-configs/{carModelId}/{configId}/price
     */
    @PutMapping("/{carModelId}/{configId}/price")
    public ResponseEntity<ApiResponse<String>> updatePrice(@PathVariable Long carModelId,
                                         @PathVariable Long configId,
                                         @RequestParam BigDecimal price) {
        boolean updated = service.updateOptionalPrice(carModelId, configId, price);
        if (updated) {
            return ResponseUtil.success("选装价格更新成功");
        } else {
            return ResponseUtil.notFound("车型配置不存在");
        }
    }

    /**
     * 更新配置评分
     * PUT /api/car-model-configs/{carModelId}/{configId}/score
     */
    @PutMapping("/{carModelId}/{configId}/score")
    public ResponseEntity<ApiResponse<String>> updateScore(@PathVariable Long carModelId,
                                         @PathVariable Long configId,
                                         @RequestParam Byte score) {
        boolean updated = service.updateScore(carModelId, configId, score);
        if (updated) {
            return ResponseUtil.success("配置评分更新成功");
        } else {
            return ResponseUtil.notFound("车型配置不存在");
        }
    }

    // ==================== 删除操作 ====================
    
    /**
     * 删除单个配置关系
     * DELETE /api/car-model-configs/{carModelId}/{configId}
     */
    @DeleteMapping("/{carModelId}/{configId}")
    public ResponseEntity<ApiResponse<String>> deleteById(@PathVariable Long carModelId,
                                        @PathVariable Long configId) {
        boolean deleted = service.deleteById(carModelId, configId);
        if (deleted) {
            return ResponseUtil.success("配置关系删除成功");
        } else {
            return ResponseUtil.notFound("车型配置不存在");
        }
    }

    /**
     * 删除某车型的所有配置
     * DELETE /api/car-model-configs/car-model/{carModelId}
     */
    @DeleteMapping("/car-model/{carModelId}")
    public ResponseEntity<ApiResponse<String>> deleteByCarModelId(@PathVariable Long carModelId) {
        service.deleteByCarModelId(carModelId);
        return ResponseUtil.success("车型所有配置删除成功");
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 检查配置关系是否存在
     * GET /api/car-model-configs/{carModelId}/{configId}/exists
     */
    @GetMapping("/{carModelId}/{configId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> exists(@PathVariable Long carModelId,
                                     @PathVariable Long configId) {
        boolean exists = service.exists(carModelId, configId);
        return ResponseUtil.success(exists);
    }

    /**
     * 创建配置请求DTO
     */
    public static class CreateConfigRequest {
        private Long carModelId;
        private Long configId;
        private Byte enabled;
        private BigDecimal optionalPrice;
        private Byte score;

        // Getters and Setters
        public Long getCarModelId() { return carModelId; }
        public void setCarModelId(Long carModelId) { this.carModelId = carModelId; }
        
        public Long getConfigId() { return configId; }
        public void setConfigId(Long configId) { this.configId = configId; }
        
        public Byte getEnabled() { return enabled; }
        public void setEnabled(Byte enabled) { this.enabled = enabled; }
        
        public BigDecimal getOptionalPrice() { return optionalPrice; }
        public void setOptionalPrice(BigDecimal optionalPrice) { this.optionalPrice = optionalPrice; }
        
        public Byte getScore() { return score; }
        public void setScore(Byte score) { this.score = score; }
    }
}
