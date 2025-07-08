package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.CarModelSalesRankingDTO;
import cn.com.undefined.abdap_backend.service.RankingService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        List<CarModelSalesRankingDTO> result = rankingService.getCarModelSalesRanking(startMonth, endMonth, region,
                top);
        return ResponseUtil.success(result);
    }
}