package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.CarModelRankingDTO;
import cn.com.undefined.abdap_backend.dto.MonthlySalesTrendDTO;
import cn.com.undefined.abdap_backend.dto.RegionDTO;
import cn.com.undefined.abdap_backend.dto.MonthlyRevenueTrendDTO;
import cn.com.undefined.abdap_backend.dto.RegionSalesDTO;
import cn.com.undefined.abdap_backend.dto.SaleRecordDTO;
import cn.com.undefined.abdap_backend.service.CarModelService;
import cn.com.undefined.abdap_backend.service.RegionService;
import cn.com.undefined.abdap_backend.service.SaleRecordService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 销售记录控制器
 */
@RestController
@RequestMapping("/api/sale-records")
@CrossOrigin(origins = "*")
public class SaleRecordController {

    @Autowired
    private SaleRecordService service;

    @Autowired
    private RegionService regionService;

    @Autowired
    private CarModelService carModelService;

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
     * 根据地区ID（市）查询销售记录
     * GET /api/sale-records?regionId={regionId}
     */
    @GetMapping(params = "regionId")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getByRegionId(@RequestParam Long regionId) {
        List<SaleRecordDTO> records = service.findByRegionId(regionId);
        return ResponseUtil.success(records);
    }

    /**
     * 根据地区名称（省）查询销售记录
     * GET /api/sale-records?regionName={regionName}
     */
    @GetMapping(params = "regionName")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getByRegionName(@RequestParam String regionName) {
        List<Long> regionIds = regionService.getRegionsByParent(regionName).stream()
                    .map(RegionDTO::getRegionId)
                    .collect(Collectors.toList());
        List<SaleRecordDTO> records = service.findByRegionIds(regionIds);
        return ResponseUtil.success(records);
    }

    /**
     * 根据车型ID和地区ID（市）查询销售记录
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
     * 根据车型ID和地区名称（省）查询销售记录
     * GET /api/sale-records?carModelId={carModelId}&regionName={regionName}
     */
    @GetMapping(params = { "carModelId", "regionName" })
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getByCarModelIdAndRegionName(
            @RequestParam Long carModelId,
            @RequestParam String regionName) {
        
        return getMultipleSaleRecords(List.of(carModelId), 
                null, 
                List.of(regionName));
    }


    /**
     * 获取指定多个车型id、多个地区id或多个省级下的销售数据
     * GET
     * /api/sale-records/multiple?carModelIds={carModelIds}&regionIds={regionIds}&regionNames={regionNames}
     */
    @GetMapping("/multiple")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> getMultipleSaleRecords(
            @RequestParam(required = false) List<Long> carModelIds,
            @RequestParam(required = false) List<Long> regionIds,
            @RequestParam(required = false) List<String> regionNames) {
        // 地区id和省级名称只能传递一个，均传递时400
        if ((regionIds != null && !regionIds.isEmpty())
                && (regionNames != null && !regionNames.isEmpty())) {
            return ResponseUtil.badRequest("地区id和省份名称只能传递一个，请检查请求参数");
        }
        if (regionNames != null && !regionNames.isEmpty()) {
            // 如果传递了省份名称，则先查询其下的所有市级地区id
            List<Long> regionIdsFromNames = regionService.getRegionsByParent(regionNames).stream()
                    .map(RegionDTO::getRegionId)
                    .collect(Collectors.toList());
            regionIds = regionIdsFromNames;
        }
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
     * },
     * {
     * "date": "2025-05",
     * "salesVolume": 1100,
     * "salesAmount": 1700000,
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
     * @param regionName 省份名
     * @return
     */
    @GetMapping("/complex-structure")
    public ResponseEntity<ApiResponse<Object>> getComplexStructureDataByRegionId(@RequestParam String regionName) {
        // 创建三种数据结构的局部内部类
        /**
         * 月度销售数据
         * 合计每个月的销售量、销售额、预测量和预测额
         */
        @AllArgsConstructor
        @Data
        class MonthlySales {
            private String date;
            private int salesVolume;
            private BigDecimal salesAmount;
        }
        /**
         * 车型销售数据
         * 合计每个车型的总销售量，展示品牌
         */
        @AllArgsConstructor
        @Data
        class BrandSales {
            private String brand;
            private String model;
            private int totalVolume;
        }
        /**
         * 市区销售数据
         * 统计指定省份下每个市区的总销售量
         */
        @AllArgsConstructor
        @Data
        class RegionSales {
            private String region;
            private int salesVolume;
        }
        @AllArgsConstructor
        @Data
        class ComplexData {
            private List<MonthlySales> monthlySales;
            private List<BrandSales> brandSales;
            private List<RegionSales> regionSales;
        }
        // 传入的地区是省级，先查询其下的所有市级地区id
        List<Long> regionIds = regionService.getRegionsByParent(regionName).stream()
                .map(RegionDTO::getRegionId)
                .collect(Collectors.toList());

        // 查询这些子地区的所有销售记录
        List<SaleRecordDTO> records = service.getMultipleSaleRecords(null, regionIds);

        // 按日期合并records，得到MonthlySales数据
        List<MonthlySales> monthlySales = records.stream()
                .collect(Collectors.groupingBy(SaleRecordDTO::getSaleMonth))
                .entrySet().stream()
                .map(entry -> {
                    return new MonthlySales(
                            entry.getKey().toString(),
                            entry.getValue().stream().mapToInt(SaleRecordDTO::getSaleCount).sum(),
                            entry.getValue().stream().map(SaleRecordDTO::getSaleAmount).reduce(BigDecimal.ZERO,
                                    BigDecimal::add));
                })
                .collect(Collectors.toList());
        // 按车型合并records，得到BrandSales数据
        List<BrandSales> brandSales = records.stream()
                .collect(Collectors.groupingBy(SaleRecordDTO::getCarModelId))
                .entrySet().stream()
                .map(entry -> {
                    return new BrandSales(
                            carModelService.getCarModelById(entry.getValue().get(0).getCarModelId()).getBrandName(), // 需要根据车型ID查询品牌
                            entry.getValue().get(0).getCarModelName(), // 假设所有记录的车型名相同
                            entry.getValue().stream().mapToInt(SaleRecordDTO::getSaleCount).sum());
                })
                .collect(Collectors.toList());
        // 按地区合并records，得到RegionSales数据
        List<RegionSales> regionSales = records.stream()
                .collect(Collectors.groupingBy(SaleRecordDTO::getRegionId))
                .entrySet().stream()
                .map(entry -> {
                    return new RegionSales(
                            entry.getValue().get(0).getRegionName(), // 假设所有记录的地区名相同
                            entry.getValue().stream().mapToInt(SaleRecordDTO::getSaleCount).sum());
                })
                .collect(Collectors.toList());

        return ResponseUtil.success(new ComplexData(monthlySales, brandSales, regionSales));
    }

    // ---以下为未验证接口---

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
