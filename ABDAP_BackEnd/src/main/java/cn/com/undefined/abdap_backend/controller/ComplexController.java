package cn.com.undefined.abdap_backend.controller;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.com.undefined.abdap_backend.repository.SaleRecordRepository;
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

    @GetMapping("/monthly-summary")
    public List<MonthlySaleSummaryDTO> getMonthlySummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String carModel) {

        // 聚合查询
        List<Object[]> rawList = saleRecordRepository.findMonthlySummary(
                startMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                endMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                region.equals("all") ? null : region,
                carModel.equals("all") ? null : carModel);

        return rawList.stream().map(arr -> new MonthlySaleSummaryDTO(
                arr[0].toString(), // month
                arr[1].toString(), // region
                arr[2].toString(), // carModel
                ((Number) arr[3]).intValue(), // saleCount
                (BigDecimal) arr[4] // saleAmount
        )).toList();
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
}
