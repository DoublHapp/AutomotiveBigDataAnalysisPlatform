package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.FuelEconomyDTO;
import cn.com.undefined.abdap_backend.service.FuelEconomyService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 燃油经济性数据控制器
 */
@RestController
@RequestMapping("/api/fuel-economy")
@CrossOrigin(origins = "*")
public class FuelEconomyController {

    @Autowired
    private FuelEconomyService fuelEconomyService;

    /**
     * 获取所有燃油经济性数据
     * 
     * @return 燃油经济性数据列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FuelEconomyDTO>>> getAllFuelEconomy() {
        List<FuelEconomyDTO> fuelEconomyList = fuelEconomyService.getAllFuelEconomy();
        return ResponseUtil.success(fuelEconomyList);
    }

    /**
     * 根据车型ID获取燃油经济性数据（一对一关系）
     * 
     * @param carModelId 车型ID
     * @return 燃油经济性数据
     */
    @GetMapping("/car-model/{carModelId}")
    public ResponseEntity<ApiResponse<FuelEconomyDTO>> getFuelEconomyByCarModelId(
            @PathVariable Long carModelId) {
        FuelEconomyDTO fuelEconomy = fuelEconomyService.getFuelEconomyByCarModelId(carModelId);
        return ResponseUtil.success(fuelEconomy);
    }

    /**
     * 根据燃料类型获取燃油经济性数据
     * 
     * @param fuelType 燃料类型
     * @return 燃油经济性数据列表
     */
    @GetMapping("/fuel-type/{fuelType}")
    public ResponseEntity<ApiResponse<List<FuelEconomyDTO>>> getFuelEconomyByFuelType(@PathVariable String fuelType) {
        List<FuelEconomyDTO> fuelEconomyList = fuelEconomyService.getFuelEconomyByFuelType(fuelType);
        return ResponseUtil.success(fuelEconomyList);
    }
}
