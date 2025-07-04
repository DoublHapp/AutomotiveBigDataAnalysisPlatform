package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.SaleRecordDTO;
import cn.com.undefined.abdap_backend.entity.SaleRecord;
import cn.com.undefined.abdap_backend.service.PredictionService;
import cn.com.undefined.abdap_backend.service.SaleRecordService;
import cn.com.undefined.abdap_backend.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预测控制器
 */
@RestController
@RequestMapping("/api/prediction")
@CrossOrigin(origins = "*")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private SaleRecordService saleRecordService;

    /**
     * 根据车型ID和地区ID使用ARIMA模型预测销量
     * GET
     * /api/prediction/ARIMA?carModelId={carModelId}&regionId={regionId}&months={months}&p={p}&d={d}&q={q}
     */
    @GetMapping("/ARIMA")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> predictSalesWithARIMA(
            @RequestParam Long carModelId,
            @RequestParam Long regionId,
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) Integer p,
            @RequestParam(required = false) Integer d,
            @RequestParam(required = false) Integer q) {

        // 获取历史销售数据
        List<SaleRecord> historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionIdRaw(carModelId, regionId);

        // 进行预测
        List<SaleRecordDTO> predictions;
        if (p != null && d != null && q != null) {
            predictions = predictionService.predictSalesWithARIMA(historicalData, months, p, d, q);
        } else {
            predictions = predictionService.predictSalesWithARIMA(historicalData, months);
        }

        return ResponseUtil.success(predictions);
    }

    /**
     * 根据车型ID和地区ID使用Prophet模型预测销量
     * GET
     * /api/prediction/Prophet?carModelId={carModelId}&regionId={regionId}&months={months}
     */
    @GetMapping("/Prophet")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> predictSalesWithProphet(
            @RequestParam Long carModelId,
            @RequestParam Long regionId,
            @RequestParam(defaultValue = "6") int months) {

        // 获取历史销售数据
        List<SaleRecord> historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionIdRaw(carModelId, regionId);

        // 进行预测
        List<SaleRecordDTO> predictions = predictionService.predictSalesWithProphet(historicalData, months);

        return ResponseUtil.success(predictions);
    }
}
