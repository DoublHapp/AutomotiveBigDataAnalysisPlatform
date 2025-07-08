package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.CarModelDTO;
import cn.com.undefined.abdap_backend.dto.CarModelSalesRankingDTO;
import cn.com.undefined.abdap_backend.repository.RankingRepository;
import cn.com.undefined.abdap_backend.repository.SaleRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RankingService {

        @Autowired
        private RankingRepository repository;

        @Autowired
        private SaleRecordRepository saleRecordRepository;

        public List<CarModelSalesRankingDTO> getCarModelSalesRanking(YearMonth startMonth, YearMonth endMonth,
                        String region, Integer top) {
                List<Object[]> results = repository.findCarModelSalesAndOpinion(
                                startMonth.atDay(1).toString(),
                                endMonth.atEndOfMonth().toString(),
                                startMonth.minusYears(1).atDay(1).toString(),
                                endMonth.minusYears(1).atEndOfMonth().toString(),
                                region.equals("all") ? null : region,
                                PageRequest.of(0, top != null ? top : 10));

                BigDecimal totalSaleCount = saleRecordRepository.findTotalSalesByMonthAndRegion(
                                startMonth.atDay(1).toString(),
                                endMonth.atEndOfMonth().toString(), region);
                return results.stream().map(arr -> {
                        // arr字段顺序见RankingRepository注释
                        CarModelDTO carModelDTO = new CarModelDTO(
                                        ((Number) arr[0]).longValue(), // carModelId
                                        (String) arr[1], // modelName
                                        (String) arr[2], // modelFullName
                                        ((Number) arr[3]).longValue(), // brandId
                                        (String) arr[4], // brandName
                                        (String) arr[5], // level
                                        arr[6] != null ? ((java.sql.Date) arr[6]).toLocalDate() : null, // launchDate
                                        arr[7] != null ? new BigDecimal(arr[7].toString()) : null, // officialPrice
                                        (String) arr[8], // engineType
                                        arr[9] != null ? ((Number) arr[9]).intValue() : null, // seatNum
                                        (String) arr[10], // driveType
                                        arr[11] != null ? ((Number) arr[11]).intValue() : null, // rangeKm
                                        (String) arr[12] // imageUrl
                        );
                        Long saleCount = arr[13] != null ? ((Number) arr[13]).longValue() : 0L;
                        Long lastYearSaleCount = arr[14] != null ? ((Number) arr[14]).longValue() : 0L;
                        Double opinionScore = arr[15] != null ? ((Number) arr[15]).doubleValue() : null;

                        // 销量增长率
                        BigDecimal saleGrowthRate = (lastYearSaleCount != null && lastYearSaleCount != 0)
                                        ? BigDecimal.valueOf(saleCount - lastYearSaleCount)
                                                        .divide(BigDecimal.valueOf(lastYearSaleCount), 4,
                                                                        RoundingMode.HALF_UP)
                                        : null;

                        // 市场份额
                        BigDecimal marketShare = (totalSaleCount.compareTo(BigDecimal.ZERO) > 0)
                                        ? BigDecimal.valueOf(saleCount).divide(totalSaleCount, 4, RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO;

                        return new CarModelSalesRankingDTO(
                                        carModelDTO,
                                        saleCount,
                                        saleGrowthRate,
                                        marketShare,
                                        opinionScore);
                }).collect(Collectors.toList());
        }
}