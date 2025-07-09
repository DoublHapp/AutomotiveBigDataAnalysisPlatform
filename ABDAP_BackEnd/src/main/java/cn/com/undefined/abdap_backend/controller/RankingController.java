package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.CarModelMarketShareRankingDTO;
import cn.com.undefined.abdap_backend.dto.CarModelMatchScoreDTO;
import cn.com.undefined.abdap_backend.dto.CarModelSalesRankingDTO;
import cn.com.undefined.abdap_backend.dto.CarModelValueRankingDTO;
import cn.com.undefined.abdap_backend.dto.FuelConsumptionRankingDTO;
import cn.com.undefined.abdap_backend.dto.RegionSalesRankingDTO;
import cn.com.undefined.abdap_backend.entity.Ranking;
import cn.com.undefined.abdap_backend.service.RankingService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/ranking")
@CrossOrigin(origins = "*")
public class RankingController {
    @Autowired
    private RankingService rankingService;

    /**
     * 车型销量排行榜接口
     * GET /api/ranking/sales?startMonth=2024-01&endMonth=2024-12&region=all&top=10
     */
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<List<CarModelSalesRankingDTO>>> getCarModelSalesRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(required = false, defaultValue = "all") String region,
            @RequestParam(required = false, defaultValue = "10") Integer top) {
        // 将所有参数拼接位一个字符串作为rankType
        String rankType = String.format("销量排行榜_%s_%s_%s_%d", startMonth, endMonth, region, top);
        Ranking ranking = rankingService.getRankingByType(rankType);
        List<CarModelSalesRankingDTO> result = null;
        if (ranking != null) {
            // 如果已经存在该排行榜，则直接返回
            List<CarModelSalesRankingDTO> cachedResult = rankingService.parseRankingDTOList(ranking,
                    CarModelSalesRankingDTO.class);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return ResponseUtil.success(cachedResult);
            }
        } else {
            result = rankingService.getCarModelSalesRanking(startMonth, endMonth, region,
                    top);
            if (!result.isEmpty()) {
                rankingService.saveRanking(result, rankType, LocalDate.now());
            }
        }

        return ResponseUtil.success(result);
    }

    /**
     * 地区销量排行榜接口
     * GET
     * /api/ranking/region-sales?startMonth=2024-01&endMonth=2024-12&region=all&top=10
     */
    @GetMapping("/region-sales")
    public ResponseEntity<ApiResponse<List<RegionSalesRankingDTO>>> getRegionSalesRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(required = false, defaultValue = "all") String region,
            @RequestParam(required = false, defaultValue = "10") Integer top) {
        // 拼接参数作为rankType
        String rankType = String.format("地区销量排行榜_%s_%s_%s_%d", startMonth, endMonth, region, top);
        Ranking ranking = rankingService.getRankingByType(rankType);
        List<RegionSalesRankingDTO> result = null;
        if (ranking != null) {
            // 已有缓存则直接返回
            List<RegionSalesRankingDTO> cachedResult = rankingService.parseRankingDTOList(ranking,
                    RegionSalesRankingDTO.class);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return ResponseUtil.success(cachedResult);
            }
        } else {
            result = rankingService.getRegionSalesRanking(startMonth, endMonth, region, top);
            if (!result.isEmpty()) {
                rankingService.saveRanking(result, rankType, LocalDate.now());
            }
        }
        return ResponseUtil.success(result);
    }

    /**
     * 油耗排行榜接口
     * GET
     * /api/ranking/fuel-consumption?level=all&maxPrice=2.1&engineType=all&top=10
     */
    @GetMapping("/fuel-consumption")
    public ResponseEntity<ApiResponse<List<FuelConsumptionRankingDTO>>> getFuelConsumptionRanking(
            @RequestParam(required = false, defaultValue = "all") String level,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, defaultValue = "all") String engineType,
            @RequestParam(required = false, defaultValue = "10") Integer top) {
        String rankType = String.format("油耗排行榜_%s_%s_%s_%d", level, maxPrice, engineType, top);
        Ranking ranking = rankingService.getRankingByType(rankType);
        List<FuelConsumptionRankingDTO> result = null;
        if (ranking != null) {
            List<FuelConsumptionRankingDTO> cachedResult = rankingService.parseRankingDTOList(ranking,
                    FuelConsumptionRankingDTO.class);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return ResponseUtil.success(cachedResult);
            }
        } else {
            result = rankingService.getFuelConsumptionRanking(level, maxPrice, engineType, top);
            if (!result.isEmpty()) {
                rankingService.saveRanking(result, rankType, LocalDate.now());
            }
        }
        return ResponseUtil.success(result);
    }

    /**
     * 购车推荐排行榜接口
     * GET
     * /api/ranking/match-score?level=all&minPrice=10.0&maxPrice=30.0&engineType=all&seatNum=5&factory=all&top=4
     */
    @GetMapping("/match-score")
    public ResponseEntity<ApiResponse<List<CarModelMatchScoreDTO>>> getCarModelMatchScoreRanking(
            @RequestParam(required = false, defaultValue = "all") String level,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, defaultValue = "all") String engineType,
            @RequestParam(required = false) Integer seatNum,
            @RequestParam(required = false, defaultValue = "all") String factory,
            @RequestParam(required = false, defaultValue = "4") Integer top) {
        // 拼接参数作为rankType
        String rankType = String.format("购车推荐榜_%s_%s_%s_%s_%s_%s_%d",
                level, minPrice, maxPrice, engineType, seatNum, factory, top);
        Ranking ranking = rankingService.getRankingByType(rankType);
        List<CarModelMatchScoreDTO> result = null;
        if (ranking != null) {
            // 已有缓存则直接返回
            List<CarModelMatchScoreDTO> cachedResult = rankingService.parseRankingDTOList(ranking,
                    CarModelMatchScoreDTO.class);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return ResponseUtil.success(cachedResult);
            }
        } else {
            result = rankingService.getCarModelMatchScoreRanking(level, minPrice, maxPrice, engineType, seatNum,
                    factory, top);
            if (!result.isEmpty()) {
                rankingService.saveRanking(result, rankType, LocalDate.now());
            }
        }
        return ResponseUtil.success(result);
    }

    /**
     * 口碑排行榜接口
     * GET
     * /api/ranking/opinion-score?startMonth=2024-01&endMonth=2024-12&region=all&top=10
     */
    @GetMapping("/opinion-score")
    public ResponseEntity<ApiResponse<List<CarModelSalesRankingDTO>>> getCarModelOpinionScoreRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(required = false, defaultValue = "all") String region,
            @RequestParam(required = false, defaultValue = "10") Integer top) {
        // 将所有参数拼接位一个字符串作为rankType
        String rankType = String.format("口碑排行榜_%s_%s_%s_%d", startMonth, endMonth, region, top);
        Ranking ranking = rankingService.getRankingByType(rankType);
        List<CarModelSalesRankingDTO> result = null;
        if (ranking != null) {
            // 如果已经存在该排行榜，则直接返回
            List<CarModelSalesRankingDTO> cachedResult = rankingService.parseRankingDTOList(ranking,
                    CarModelSalesRankingDTO.class);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return ResponseUtil.success(cachedResult);
            }
        } else {
            result = rankingService.getCarModelOpinionScoreRanking(startMonth, endMonth, region,
                    top);
            if (!result.isEmpty()) {
                rankingService.saveRanking(result, rankType, LocalDate.now());
            }
        }

        return ResponseUtil.success(result);
    }

    /**
     * 市场份额排行榜接口
     * GET
     * /api/ranking/market-share?startMonth=2024-01&endMonth=2024-12&region=all&top=10
     */
    @GetMapping("/market-share")
    public ResponseEntity<ApiResponse<List<CarModelMarketShareRankingDTO>>> getCarModelMarketShareRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(required = false, defaultValue = "all") String region,
            @RequestParam(required = false, defaultValue = "10") Integer top) {
        String rankType = String.format("市场份额排行榜_%s_%s_%s_%d", startMonth, endMonth, region, top);
        Ranking ranking = rankingService.getRankingByType(rankType);
        List<CarModelMarketShareRankingDTO> result = null;
        if (ranking != null) {
            List<CarModelMarketShareRankingDTO> cachedResult = rankingService.parseRankingDTOList(ranking,
                    CarModelMarketShareRankingDTO.class);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return ResponseUtil.success(cachedResult);
            }
        } else {
            result = rankingService.getCarModelMarketShareRanking(startMonth, endMonth, region, top);
            if (!result.isEmpty()) {
                rankingService.saveRanking(result, rankType, LocalDate.now());
            }
        }
        return ResponseUtil.success(result);
    }

    /**
     * 增长率排行榜接口
     * GET
     * /api/ranking/growth-rate?startMonth=2024-01&endMonth=2024-12&region=all&top=10
     */
    @GetMapping("/growth-rate")
    public ResponseEntity<ApiResponse<List<CarModelMarketShareRankingDTO>>> getCarModelGrowthRateRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(required = false, defaultValue = "all") String region,
            @RequestParam(required = false, defaultValue = "10") Integer top) {
        String rankType = String.format("增长率排行榜_%s_%s_%s_%d", startMonth, endMonth, region, top);
        Ranking ranking = rankingService.getRankingByType(rankType);
        List<CarModelMarketShareRankingDTO> result = null;
        if (ranking != null) {
            List<CarModelMarketShareRankingDTO> cachedResult = rankingService.parseRankingDTOList(ranking,
                    CarModelMarketShareRankingDTO.class);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return ResponseUtil.success(cachedResult);
            }
        } else {
            result = rankingService.getCarModelGrowthRateRanking(startMonth, endMonth, region, top);
            if (!result.isEmpty()) {
                rankingService.saveRanking(result, rankType, LocalDate.now());
            }
        }
        return ResponseUtil.success(result);
    }

    /**
     * 性价比排行榜接口
     * GET /api/ranking/value?startMonth=2024-01&endMonth=2024-12&region=all&top=10
     */
    @GetMapping("/value")
    public ResponseEntity<ApiResponse<List<CarModelValueRankingDTO>>> getCarModelValueRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(required = false, defaultValue = "all") String region,
            @RequestParam(required = false, defaultValue = "10") Integer top) {
        String rankType = String.format("性价比排行榜_%s_%s_%s_%d", startMonth, endMonth, region, top);
        Ranking ranking = rankingService.getRankingByType(rankType);
        List<CarModelValueRankingDTO> result = null;
        if (ranking != null) {
            List<CarModelValueRankingDTO> cachedResult = rankingService.parseRankingDTOList(ranking,
                    CarModelValueRankingDTO.class);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                return ResponseUtil.success(cachedResult);
            }
        } else {
            result = rankingService.getCarModelValueRanking(startMonth, endMonth, region, top);
            if (!result.isEmpty()) {
                rankingService.saveRanking(result, rankType, LocalDate.now());
            }
        }
        return ResponseUtil.success(result);
    }
}