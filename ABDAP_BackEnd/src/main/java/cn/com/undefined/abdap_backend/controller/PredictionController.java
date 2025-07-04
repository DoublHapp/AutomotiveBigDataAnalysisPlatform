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
     * 根据车型ID和地区ID预测销量
     * GET /api/prediction?carModelId={carModelId}&regionId={regionId}&months={months}
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleRecordDTO>>> predictSales(
            @RequestParam Long carModelId,
            @RequestParam Long regionId,
            @RequestParam(defaultValue = "6") int months) {
        
        // 获取历史销售数据
        List<SaleRecord> historicalData = saleRecordService.getSaleRecordsByCarModelIdAndRegionId(carModelId, regionId)
                .stream()
                .map(dto -> {
                    SaleRecord record = new SaleRecord();
                    record.setSaleId(dto.getSaleId());
                    record.setCarModelId(dto.getCarModelId());
                    record.setRegionId(dto.getRegionId());
                    record.setSaleMonth(dto.getSaleMonth());
                    record.setSaleCount(dto.getSaleCount());
                    record.setSaleAmount(dto.getSaleAmount());
                    return record;
                })
                .toList();
        
        // 进行预测
        List<SaleRecordDTO> predictions = predictionService.predictSalesWithARIMA(historicalData, months);
        
        return ResponseUtil.success(predictions);
    }
}
