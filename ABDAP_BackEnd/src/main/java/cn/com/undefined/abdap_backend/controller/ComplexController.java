package cn.com.undefined.abdap_backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.entity.Ranking;
import cn.com.undefined.abdap_backend.repository.SaleRecordRepository;
import cn.com.undefined.abdap_backend.service.RankingService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 复合控制器
 */
@RestController
@RequestMapping("/api/complex")
@CrossOrigin(origins = "*")
public class ComplexController {
    @Autowired
    private SaleRecordRepository saleRecordRepository;

    @Autowired
    private RankingService rankingService;

    /**
     * 获取月度销售汇总
     * GET
     * /api/complex/monthly-summary?startMonth=2023-01&endMonth=2023-12&region=all&carModel=Toyota
     * 
     * @param startMonth
     * @param endMonth
     * @param region
     * @param carModel
     * @return
     */
    @GetMapping("/monthly-summary")
    public ResponseEntity<ApiResponse<List<MonthlySaleSummaryDTO>>> getMonthlySummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String carModel) {

        // 聚合查询
        List<Object[]> rawList = saleRecordRepository.findMonthlySummary(
                startMonth.atDay(1).toString(),
                endMonth.atEndOfMonth().toString(),
                region.equals("all") ? null : region,
                carModel.equals("all") ? null : carModel);

        return ResponseUtil.success(rawList.stream().map(arr -> new MonthlySaleSummaryDTO(
                arr[0].toString(), // month
                arr[1].toString(), // region
                arr[2].toString(), // carModel
                ((Number) arr[3]).intValue(), // saleCount
                (BigDecimal) arr[4] // saleAmount
        )).toList());
    }

    /**
     * 获取市场份额汇总
     * GET
     * /api/complex/market-share-summary?startMonth=2023-01&endMonth=2023-12&region=all&carModel=Toyota
     * 
     * @param startMonth 起始月份
     * @param endMonth   结束月份
     * @param region     地区（可选）
     * @param carModel   车型
     * @return 市场份额汇总信息
     */
    @GetMapping("/market-share-summary")
    public ResponseEntity<ApiResponse<MarketShareSummaryDTO>> getMarketShareSummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(defaultValue = "all") String region,
            @RequestParam String carModel) {

        // 拼接参数作为rankType
        String rankType = String.format("market_share_%s_%s_%s_%s", startMonth, endMonth, region, carModel);
        // 将该对象当做Ranking缓存与读取，以提高效率
        Ranking ranking = rankingService.getRankingByType(rankType);
        MarketShareSummaryDTO dto = null;
        if (ranking != null) {
            // 已有缓存则直接返回
            List<MarketShareSummaryDTO> cachedResult = rankingService.parseRankingDTOList(ranking,
                    MarketShareSummaryDTO.class);
            if (cachedResult != null && !cachedResult.isEmpty()) {
                dto = cachedResult.get(0);
            }
        } else {
            List<Object[]> result = saleRecordRepository.findMarketShareSummary(
                    startMonth.atDay(1).toString(),
                    endMonth.atEndOfMonth().toString(),
                    "all".equalsIgnoreCase(region) ? null : region,
                    carModel);
            if (result != null && !result.isEmpty()) {
                Object[] arr = result.get(0);
                dto = new MarketShareSummaryDTO(
                        arr[0] != null ? new BigDecimal(arr[0].toString()) : BigDecimal.ZERO,
                        arr[1] != null ? ((Number) arr[1]).intValue() : 0,
                        arr[2] != null ? new BigDecimal(arr[2].toString()) : BigDecimal.ZERO);
            }
            rankingService.saveRanking(List.of(dto), rankType, LocalDate.now());
        }
        return ResponseUtil.success(dto);
    }

    // DTO定义
    @Data
    @AllArgsConstructor
    public static class MonthlySaleSummaryDTO {
        private String month;
        private String region;
        private String carModel;
        private Integer saleCount;
        private BigDecimal saleAmount;
    }

    @Data
    @AllArgsConstructor
    public static class MarketShareSummaryDTO {
        /**
         * 市场份额占比（如0.1234表示12.34%）
         */
        private BigDecimal marketShare;

        /**
         * 指定地区的总销售量
         */
        private Integer totalSaleCount;

        /**
         * 指定地区的总销售额
         */
        private BigDecimal totalSaleAmount;
    }
}
