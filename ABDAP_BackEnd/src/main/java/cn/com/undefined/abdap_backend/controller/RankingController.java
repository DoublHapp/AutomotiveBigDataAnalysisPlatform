package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.CarModelSalesRankingDTO;
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
            List<CarModelSalesRankingDTO> cachedResult = rankingService.parseRankingDTOList(ranking, CarModelSalesRankingDTO.class);
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
            List<RegionSalesRankingDTO> cachedResult = rankingService.parseRankingDTOList(ranking, RegionSalesRankingDTO.class);
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
}