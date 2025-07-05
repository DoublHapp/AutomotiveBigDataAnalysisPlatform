package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.RegionDTO;
import cn.com.undefined.abdap_backend.service.RegionService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地区数据控制器
 * 提供地区相关的REST API接口
 */
@RestController
@RequestMapping("/api/regions")
@CrossOrigin(origins = "*")
public class RegionController {

    @Autowired
    private RegionService regionService;

    /**
     * 获取所有地区数据（市级）
     * 
     * @return 地区数据列表
     */
    @Deprecated
    @GetMapping
    public ResponseEntity<ApiResponse<List<RegionDTO>>> getAllRegions() {
        List<RegionDTO> regions = regionService.getAllRegions();
        return ResponseUtil.success(regions);
    }

    /**
     * 根据父级地区名称获取子地区数据
     * 
     * @param parentRegion 父级地区名称
     * @return 子地区数据列表
     */
    @GetMapping("/parent/{parentRegion}")
    public ResponseEntity<ApiResponse<List<RegionDTO>>> getRegionsByParent(@PathVariable String parentRegion) {
        List<RegionDTO> regions = regionService.getRegionsByParent(parentRegion);
        return ResponseUtil.success(regions);
    }

    /**
     * 获取所有顶级地区数据（省）
     * 
     * @return 顶级地区数据列表
     */
    @GetMapping("/top-level")
    public ResponseEntity<ApiResponse<List<String>>> getTopLevelRegions() {
        List<String> regions = regionService.getTopLevelRegions();
        return ResponseUtil.success(regions);
    }

    /**
     * 获取所有非顶级地区数据（市）
     * 
     * @return 非顶级地区数据列表
     */
    @GetMapping("/non-top-level")
    public ResponseEntity<ApiResponse<List<RegionDTO>>> getNonTopLevelRegions() {
        List<RegionDTO> regions = regionService.getAllRegions();
        return ResponseUtil.success(regions);
    }
}
