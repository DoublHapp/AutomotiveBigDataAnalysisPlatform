package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.CarModelDTO;
import cn.com.undefined.abdap_backend.dto.CarModelSalesRankingDTO;
import cn.com.undefined.abdap_backend.dto.RegionDTO;
import cn.com.undefined.abdap_backend.dto.RegionSalesRankingDTO;
import cn.com.undefined.abdap_backend.entity.Ranking;
import cn.com.undefined.abdap_backend.repository.RankingRepository;
import cn.com.undefined.abdap_backend.repository.SaleRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RankingService {

        @Autowired
        private RankingRepository repository;

        @Autowired
        private SaleRecordRepository saleRecordRepository;

        @Autowired
        private ObjectMapper objectMapper;

        /**
         * 获取车型销量排名
         * 
         * @param startMonth
         * @param endMonth
         * @param region
         * @param top
         * @return
         */
        public List<CarModelSalesRankingDTO> getCarModelSalesRanking(YearMonth startMonth, YearMonth endMonth,
                        String region, Integer top) {
                List<Object[]> results = repository.findCarModelSalesAndOpinion(
                                startMonth.atDay(1).toString(),
                                endMonth.atEndOfMonth().toString(),
                                startMonth.minusYears(1).atDay(1).toString(),
                                endMonth.minusYears(1).atEndOfMonth().toString(),
                                normalizeRegionParam(region),
                                PageRequest.of(0, top != null ? top : 10));

                BigDecimal totalSaleCount = saleRecordRepository.findTotalSalesByMonthAndRegion(
                                startMonth.atDay(1).toString(),
                                endMonth.atEndOfMonth().toString(), normalizeRegionParam(region));
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

        /**
         * 获取地区销量排名
         * 
         * @param startMonth
         * @param endMonth
         * @param region    地区（省份或"all"表示全国）
         * @param top       返回前N条数据
         * @return 地区销量排名DTO列表
         */
        public List<RegionSalesRankingDTO> getRegionSalesRanking(YearMonth startMonth, YearMonth endMonth,
                        String region, Integer top) {
                List<Object[]> results;
                // 判断是全国（未指定省份）还是某省下的市
                if (region == null || "all".equalsIgnoreCase(region)) {
                        // 全国：按省分组
                        results = repository.findRegionSalesRanking(
                                        startMonth.atDay(1).toString(),
                                        endMonth.atEndOfMonth().toString(),
                                        startMonth.minusYears(1).atDay(1).toString(),
                                        endMonth.minusYears(1).atEndOfMonth().toString(),
                                PageRequest.of(0, top != null ? top : 10));
                } else {
                        // 某省：按该省下所有市分组
                        results = repository.findRegionSalesRanking(
                                        startMonth.atDay(1).toString(),
                                        endMonth.atEndOfMonth().toString(),
                                        startMonth.minusYears(1).atDay(1).toString(),
                                        endMonth.minusYears(1).atEndOfMonth().toString(),
                                        normalizeRegionParam(region),
                                PageRequest.of(0, top != null ? top : 10));
                }

                // 计算总销量（用于市场份额）
                BigDecimal totalSaleCount = saleRecordRepository.findTotalSalesByMonthAndRegion(
                                startMonth.atDay(1).toString(),
                                endMonth.atEndOfMonth().toString(), normalizeRegionParam(region));

                // 组装DTO
                return results.stream().map(arr -> {
                        // 字段顺序见Repository注释
                        RegionDTO regionDTO = new RegionDTO(
                                        arr[0] != null ? ((Number) arr[0]).longValue() : ((String) arr[1]).hashCode(), // regionId，省份虚拟id
                                        (String) arr[1], // regionName
                                        arr[2] != null ? (String) arr[2] : null); // parentRegion
                        Long saleCount = arr[3] != null ? ((Number) arr[3]).longValue() : 0L;
                        Long lastYearSaleCount = arr[4] != null
                                        ? ((Number) arr[4]).longValue()
                                        : 0L;

                        // 增长率
                        BigDecimal saleGrowthRate = (lastYearSaleCount != null && lastYearSaleCount != 0)
                                        ? BigDecimal.valueOf(saleCount - lastYearSaleCount)
                                                        .divide(BigDecimal.valueOf(lastYearSaleCount), 4,
                                                                        RoundingMode.HALF_UP)
                                        : null;

                        // 市场份额
                        BigDecimal marketShare = (totalSaleCount.compareTo(BigDecimal.ZERO) > 0)
                                        ? BigDecimal.valueOf(saleCount).divide(totalSaleCount, 4,
                                                        RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO;

                        return new RegionSalesRankingDTO(regionDTO, saleCount, saleGrowthRate, marketShare);
                }).collect(Collectors.toList());
        }

        private String normalizeRegionParam(String region) {
                return "all".equalsIgnoreCase(region) ? null : region;
        }

        /**
         * 解析排行榜DTO列表（泛型方法，可解析任意类型的排行榜数据）
         */
        public <T> List<T> parseRankingDTOList(Ranking ranking, Class<T> clazz) {
                if (ranking == null || ranking.getRankingData() == null) {
                        return Collections.emptyList();
                }
                try {
                        return objectMapper.readValue(
                                        ranking.getRankingData(),
                                        objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
                } catch (Exception e) {
                        e.printStackTrace();
                        return Collections.emptyList();
                }
        }

        /**
         * 存储Ranking实体（泛型方法，可存储任意类型的排行榜数据）
         */
        public <T> void saveRanking(List<T> result, String rankType, LocalDate rankTime) {
                try {
                        String rankingData = objectMapper.writeValueAsString(result);
                        Ranking ranking = new Ranking();
                        ranking.setRankType(rankType);
                        ranking.setRankTime(rankTime);
                        ranking.setRankingData(rankingData);
                        repository.save(ranking);
                } catch (JsonProcessingException e) {
                        // 处理序列化异常
                        e.printStackTrace();
                        System.err.println("\n\n\nError saving ranking data: " + e.getMessage() + "\n\n\n");
                }
        }

        /**
         * 查询指定type的排行榜数据
         */
        public Ranking getRankingByType(String rankType) {
                return repository.findByRankType(rankType);
        }
}