package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.CarModelDTO;
import cn.com.undefined.abdap_backend.dto.CarModelDetailDTO;
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
@RequestMapping("/api/car-models")
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
     * 获取分页车型列表
     * GET /api/car-models/page?page=0&size=10
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<List<CarModelDTO>>> getCarModelsByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<CarModelDTO> carModels = carModelService.getCarModelsByPage(page, size);
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

    /**
     * 车型关键字模糊搜索
     * GET /api/car-models/search?keyword=xxx&limit=100
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CarModelDTO>>> searchCarModels(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "100") int limit) {
        List<CarModelDTO> carModels = carModelService.searchCarModels(keyword, limit);
        return ResponseUtil.success(carModels);
    }

    /**
     * 获取所有车型级别
     * GET /api/car-models/levels
     */
    @GetMapping("/levels")
    public ResponseEntity<ApiResponse<List<String>>> getAllLevels() {
        List<String> levels = carModelService.getAllLevels();
        return ResponseUtil.success(levels);
    }

    /**
     * 获取所有车型发动机类型
     * GET /api/car-models/engine-types
     */
    @GetMapping("/engine-types")
    public ResponseEntity<ApiResponse<List<String>>> getAllEngineTypes() {
        List<String> engineTypes = carModelService.getAllEngineTypes();
        return ResponseUtil.success(engineTypes);
    }

    /**
     * 模糊匹配厂商列表
     * GET /api/car-models/factorys?keyword=xxx
     */
    @GetMapping("/factorys")
    public ResponseEntity<ApiResponse<List<String>>> getFactorysByKeyword(
            @RequestParam(defaultValue = "all") String keyword) {
        List<String> factorys = carModelService.getFactorysByKeyword(keyword);
        return ResponseUtil.success(factorys);
    }

    /**
     * 获取车型详情
     * GET /api/car-models/detail/{carModelId}
     * @param carModelId
     * @return
     */
    @GetMapping("detail/{carModelId}")
    public ResponseEntity<ApiResponse<CarModelDetailDTO>> getCarModelDetailDTOById(
            @PathVariable Long carModelId) {
        CarModelDetailDTO detail = carModelService.getCarModelDetailDTOById(carModelId);
        return ResponseUtil.success(detail);
    }
}
