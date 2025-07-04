package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.CarModelRankingDTO;
import cn.com.undefined.abdap_backend.dto.MonthlySalesTrendDTO;
import cn.com.undefined.abdap_backend.dto.MonthlyRevenueTrendDTO;
import cn.com.undefined.abdap_backend.dto.RegionSalesDTO;
import cn.com.undefined.abdap_backend.dto.SaleRecordDTO;
import cn.com.undefined.abdap_backend.service.SaleRecordService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


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
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getAllSaleRecords() {
        List<SaleRecordDTO> records = service.findAll();
        return ResponseUtil.success(records);
    }

    /**
     * 根据车型ID查询销售记录
     * GET /api/sale-records?carModelId={carModelId}
     */
    @GetMapping(params = "carModelId")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getByCarModelId(@RequestParam Long carModelId) {
        List<SaleRecordDTO> records = service.findByCarModelId(carModelId);
        return ResponseUtil.success(records);
    }

    /**
     * 根据地区ID查询销售记录
     * GET /api/sale-records?regionId={regionId}
     */
    @GetMapping(params = "regionId")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getByRegionId(@RequestParam Long regionId) {
        List<SaleRecordDTO> records = service.findByRegionId(regionId);
        return ResponseUtil.success(records);
    }

    /**
     * 根据车型ID和地区ID查询销售记录
     * GET /api/sale-records?carModelId={carModelId}&regionId={regionId}
     */
    @GetMapping(params = { "carModelId", "regionId" })
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getByCarModelIdAndRegionId(
            @RequestParam Long carModelId,
            @RequestParam Long regionId) {
        List<SaleRecordDTO> records = service.getSaleRecordsByCarModelIdAndRegionId(carModelId, regionId);
        return ResponseUtil.success(records);
    }

    /**
     * 获取指定多个车型id、多个地区id下的销售数据
     * GET /api/sale-records/multiple?carModelIds={carModelIds}&regionIds={regionIds}
     */
    @GetMapping("/multiple")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getMultipleSaleRecords(
            @RequestParam List<Long> carModelIds,
            @RequestParam List<Long> regionIds) {
        List<SaleRecordDTO> records = service.getMultipleSaleRecords(carModelIds, regionIds);
        return ResponseUtil.success(records);
    }

    /**
     * 
     * 按照如下数据结构的查询和返回：
     * 请求参数：
     * {
     * "regionId": "1",
     * }
     * 返回的data结构：
     * "monthlySales": [
     * {
     * "date": "2025-04",
     * "salesVolume": 1200,
     * "salesAmount": 1800000,
     * "predictedVolume": 1250,
     * "predictedAmount": 1900000
     * },
     * {
     * "date": "2025-05",
     * "salesVolume": 1100,
     * "salesAmount": 1700000,
     * "predictedVolume": 1150,
     * "predictedAmount": 1750000
     * }
     * ],
     * "brandSales": [
     * {
     * "brand": "丰田",
     * "model": "卡罗拉",
     * "totalVolume": 300
     * },
     * {
     * "brand": "比亚迪",
     * "model": "汉",
     * "totalVolume": 260
     * }
     * ],
     * "regionSales": [
     * {
     * "region": "广州市",
     * "salesVolume": 600
     * },
     * {
     * "region": "深圳市",
     * "salesVolume": 500
     * }
     * ]
     * 
     * @param regionId 省份ID
     * @return
     */
    // TODO: 实现复杂数据结构的查询和返回
    // ---以下为未验证接口---
    @GetMapping("/complex-structure")
    public ResponseEntity<ApiResponse<Object>> getComplexStructureDataByRegionId(@RequestParam Long regionId) {
        // 创建三种数据结构的局部内部类
        /**
         * 月度销售数据
         * 合计每个月的销售量、销售额、预测量和预测额
         */
        class MonthlySales {
            private String date;
            private int salesVolume;
            private int salesAmount;
            private int predictedVolume;
            private int predictedAmount;
        }
        /**
         * 车型销售数据
         * 合计每个车型的总销售量，展示品牌
         */
        class BrandSales {
            private String brand;
            private String model;
            private int totalVolume;
        }
        /**
         * 市区销售数据
         * 统计指定省份下每个市区的总销售量
         */
        class RegionSales {
            private String region;
            private int salesVolume;
        }
        class ComplexData {
            private List<MonthlySales> monthlySales;
            private List<BrandSales> brandSales;
            private List<RegionSales> regionSales;
        }
        // TODO: 综合查询逻辑，获取数据并填充到 ComplexData 对象中
        return ResponseUtil.success(new ComplexData());
    }

    /**
     * 根据地区名称查询销售记录
     * GET /api/sale-records/region/name/{regionName}
     */
    @GetMapping("/region/name/{regionName}")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getByRegionName(@PathVariable String regionName) {
        List<SaleRecordDTO> records = service.findByRegionName(regionName);
        return ResponseUtil.success(records);
    }

    /**
     * 根据车型ID和地区名称查询销售记录
     * GET
     * /api/sale-records/search-by-name?carModelId={carModelId}&regionName={regionName}
     */
    @GetMapping("/search-by-name")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getByCarModelIdAndRegionName(
            @RequestParam Long carModelId,
            @RequestParam String regionName) {
        List<SaleRecordDTO> records = service.findByCarModelIdAndRegionName(carModelId, regionName);
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
