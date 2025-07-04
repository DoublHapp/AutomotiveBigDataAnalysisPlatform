package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.CarModelRankingDTO;
import cn.com.undefined.abdap_backend.dto.MonthlySalesTrendDTO;
import cn.com.undefined.abdap_backend.dto.MonthlyRevenueTrendDTO;
import cn.com.undefined.abdap_backend.dto.RegionSalesDTO;
import cn.com.undefined.abdap_backend.entity.SaleRecord;
import cn.com.undefined.abdap_backend.service.SaleRecordService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 销售记录控制器
 */
@RestController
@RequestMapping("/api/sale-records")
@CrossOrigin(origins = "*")
public class SaleRecordController {

    @Autowired
    private SaleRecordService service;

    /**
     * 查询所有销售记录
     * GET /api/sale-records
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleRecord>>> getAllSaleRecords() {
        List<SaleRecord> records = service.findAll();
        return ResponseUtil.success(records);
    }

    /**
     * 根据车型ID查询销售记录
     * GET /api/sale-records/car-model/{carModelId}
     */
    @GetMapping("/car-model/{carModelId}")
    public ResponseEntity<ApiResponse<List<SaleRecord>>> getByCarModelId(@PathVariable Long carModelId) {
        List<SaleRecord> records = service.findByCarModelId(carModelId);
        return ResponseUtil.success(records);
    }

    /**
     * 根据地区ID查询销售记录
     * GET /api/sale-records/region/{regionId}
     */
    @GetMapping("/region/{regionId}")
    public ResponseEntity<ApiResponse<List<SaleRecord>>> getByRegionId(@PathVariable Long regionId) {
        List<SaleRecord> records = service.findByRegionId(regionId);
        return ResponseUtil.success(records);
    }

    /**
     * 根据地区名称查询销售记录
     * GET /api/sale-records/region/name/{regionName}
     */
    @GetMapping("/region/name/{regionName}")
    public ResponseEntity<ApiResponse<List<SaleRecord>>> getByRegionName(@PathVariable String regionName) {
        List<SaleRecord> records = service.findByRegionName(regionName);
        return ResponseUtil.success(records);
    }

    /**
     * 根据车型ID和地区名称查询销售记录
     * GET /api/sale-records/search?carModelId={carModelId}&regionName={regionName}
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SaleRecord>>> getByCarModelIdAndRegionName(
            @RequestParam Long carModelId,
            @RequestParam String regionName) {
        List<SaleRecord> records = service.findByCarModelIdAndRegionName(carModelId, regionName);
        return ResponseUtil.success(records);
    }

    /**
     * 获取热门车型排行榜
     * GET /api/sale-records/rankings/car-models?limit=10&timeSpan=year
     */
    @GetMapping("/rankings/car-models")
    public ResponseEntity<ApiResponse<List<CarModelRankingDTO>>> getCarModelRanking(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "year") String timeSpan) {
        
        // 根据时间跨度计算开始日期
        LocalDate startDate = service.calculateStartDate(timeSpan);
        List<CarModelRankingDTO> rankings = service.getCarModelRanking(limit, startDate);
        return ResponseUtil.success(rankings);
    }
    
    /**
     * 获取地区销量分布
     * GET /api/sale-records/distribution/regions?timeSpan=year
     */
    @GetMapping("/distribution/regions")
    public ResponseEntity<ApiResponse<List<RegionSalesDTO>>> getRegionSalesDistribution(
            @RequestParam(defaultValue = "year") String timeSpan) {
        
        // 根据时间跨度计算开始日期
        LocalDate startDate = service.calculateStartDate(timeSpan);
        List<RegionSalesDTO> distributions = service.getRegionSalesDistribution(startDate);
        return ResponseUtil.success(distributions);
    }
    
    /**
     * 获取指定车型的月度销量趋势
     * GET /api/sale-records/trends/sales/{carModelId}?timeSpan=year
     */
    @GetMapping("/trends/sales/{carModelId}")
    public ResponseEntity<ApiResponse<List<MonthlySalesTrendDTO>>> getMonthlySalesTrend(
            @PathVariable Long carModelId,
            @RequestParam(defaultValue = "year") String timeSpan) {
        
        // 根据时间跨度计算开始日期
        LocalDate startDate = service.calculateStartDate(timeSpan);
        List<MonthlySalesTrendDTO> trends = service.getMonthlySalesTrend(carModelId, startDate);
        return ResponseUtil.success(trends);
    }
    
    /**
     * 获取指定车型的月度销售额趋势
     * GET /api/sale-records/trends/revenue/{carModelId}?timeSpan=year
     */
    @GetMapping("/trends/revenue/{carModelId}")
    public ResponseEntity<ApiResponse<List<MonthlyRevenueTrendDTO>>> getMonthlyRevenueTrend(
            @PathVariable Long carModelId,
            @RequestParam(defaultValue = "year") String timeSpan) {
        
        // 根据时间跨度计算开始日期
        LocalDate startDate = service.calculateStartDate(timeSpan);
        List<MonthlyRevenueTrendDTO> trends = service.getMonthlyRevenueTrend(carModelId, startDate);
        return ResponseUtil.success(trends);
    }
}
