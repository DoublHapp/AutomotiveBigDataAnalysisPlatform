package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.CarModelDTO;
import cn.com.undefined.abdap_backend.service.CarModelService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车型控制器
 * 提供车型相关的API接口
 */
@RestController
@RequestMapping("/api/carModels")
@CrossOrigin(origins = "*")
public class CarModelController {
    
    @Autowired
    private CarModelService carModelService;
    
    /**
     * 获取车型列表
     * GET /api/carModels
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CarModelDTO>>> getCarModels() {
        List<CarModelDTO> carModels = carModelService.getAllCarModels();
        return ResponseUtil.success(carModels);
    }
    
    /**
     * 获取特定车型详情
     * GET /api/carModels/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarModelDTO>> getCarModel(@PathVariable Long id) {
        CarModelDTO carModel = carModelService.getCarModelById(id);
        return ResponseUtil.success(carModel);
    }
}
