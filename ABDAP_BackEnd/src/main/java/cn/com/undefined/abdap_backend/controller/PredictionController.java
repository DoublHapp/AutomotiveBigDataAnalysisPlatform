package cn.com.undefined.abdap_backend.controller;

import cn.com.undefined.abdap_backend.dto.ARIMAResultDTO;
import cn.com.undefined.abdap_backend.dto.ApiResponse;
import cn.com.undefined.abdap_backend.dto.ProphetResultDTO;
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
     * 根据车型ID、地区ID或地区名称使用ARIMA模型预测销量
     * GET
     * /api/prediction/ARIMA?carModelId={carModelId}&regionId={regionId}&regionName={regionName}&months={months}&p={p}&d={d}&q={q}
     */
    @GetMapping("/ARIMA")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> predictSalesWithARIMA(
            @RequestParam Long carModelId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String regionName,
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) Integer p,
            @RequestParam(required = false) Integer d,
            @RequestParam(required = false) Integer q) {

        // regionId和regionName不能同时存在
        if (regionId != null && regionName != null) {
            return ResponseUtil.badRequest("regionId和regionName不能同时存在");
        }
        // 获取历史销售数据
        List<SaleRecord> historicalData;
        if (regionId != null) {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionIdRaw(carModelId, regionId);
        } else if (regionName != null) {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionNameRaw(carModelId, regionName);
        } else {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdRaw(carModelId);
        }

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
     * 根据车型ID、地区ID或地区名称使用ARIMA模型预测销量，返回完整的预测结果
     * GET
     * /api/prediction/ARIMA/detail?carModelId={carModelId}&regionId={regionId}&regionName={regionName}&months={months}&p={p}&d={d}&q={q}
     */
    @GetMapping("/ARIMA/detail")
    public ResponseEntity<ApiResponse<ARIMAResultDTO>> predictSalesWithARIMAWithDetail(
            @RequestParam Long carModelId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String regionName,
            @RequestParam(defaultValue = "6") int months,
            @RequestParam(required = false) Integer p,
            @RequestParam(required = false) Integer d,
            @RequestParam(required = false) Integer q) {

        // regionId和regionName不能同时存在
        if (regionId != null && regionName != null) {
            return ResponseUtil.badRequest("regionId和regionName不能同时存在");
        }

        // 获取历史销售数据
        List<SaleRecord> historicalData;
        if (regionId != null) {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionIdRaw(carModelId, regionId);
        } else if (regionName != null) {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionNameRaw(carModelId, regionName);
        } else {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdRaw(carModelId);
        }

        // 进行预测
        ARIMAResultDTO arimaResult;
        if (p != null && d != null && q != null) {
            arimaResult = predictionService.predictSalesWithARIMADetail(historicalData, months, p, d, q);
        } else {
            arimaResult = predictionService.predictSalesWithARIMADetail(historicalData, months);
        }
        return ResponseUtil.success(arimaResult);
    }

    /**
     * 根据车型ID、地区ID或地区名称使用Prophet模型预测销量
     * GET
     * /api/prediction/Prophet?carModelId={carModelId}&regionId={regionId}&regionName={regionName}&months={months}
     */
    @GetMapping("/Prophet")
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> predictSalesWithProphet(
            @RequestParam Long carModelId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String regionName,
            @RequestParam(defaultValue = "6") int months) {

        // regionId和regionName不能同时存在
        if (regionId != null && regionName != null) {
            return ResponseUtil.badRequest("regionId和regionName不能同时存在");
        }

        // 获取历史销售数据
        List<SaleRecord> historicalData;
        if (regionId != null) {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionIdRaw(carModelId, regionId);
        } else if (regionName != null) {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionNameRaw(carModelId, regionName);
        } else {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdRaw(carModelId);
        }

        // 进行预测
        List<SaleRecordDTO> predictions = predictionService.predictSalesWithProphet(historicalData, months);

        return ResponseUtil.success(predictions);
    }

    /**
     * 根据车型ID、地区ID或地区名称使用Prophet模型预测销量，返回完整的预测结果
     * GET
     * /api/prediction/Prophet/detail?carModelId={carModelId}&regionId={regionId}&regionName={regionName}&months={months}
     */
    @GetMapping("/Prophet/detail")
    public ResponseEntity<ApiResponse<ProphetResultDTO>> predictSalesWithProphetWithDetail(
            @RequestParam Long carModelId,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String regionName,
            @RequestParam(defaultValue = "6") int months) {

        // regionId和regionName不能同时存在
        if (regionId != null && regionName != null) {
            return ResponseUtil.badRequest("regionId和regionName不能同时存在");
        }

        // 获取历史销售数据
        List<SaleRecord> historicalData;
        if (regionId != null) {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionIdRaw(carModelId, regionId);
        } else if (regionName != null) {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionNameRaw(carModelId, regionName);
        } else {
            historicalData = saleRecordService.getSaleRecordsByCarModelIdRaw(carModelId);
        }

        // 进行预测
        ProphetResultDTO prophetResult = predictionService.predictSalesWithProphetDetail(historicalData, months);

        return ResponseUtil.success(prophetResult);
    }
}
