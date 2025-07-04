package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.SaleRecordDTO;
import cn.com.undefined.abdap_backend.entity.SaleRecord;
import cn.com.undefined.abdap_backend.util.ARIMAUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预测服务类
 * 负责处理汽车销量预测相关的业务逻辑
 * 
 * 主要功能：
 * 1. 数据格式转换：将SaleRecord数据转换为ARIMA模型所需的数值数组
 * 2. 调用时间序列预测工具：使用ARIMAUtil进行销量预测
 * 3. 预测结果处理：对预测结果进行后处理和格式化
 * 
 */
@Service
public class PredictionService {
    /**
     * 基于销售记录进行ARIMA时间序列预测
     * 
     * @param saleRecords      销售记录列表，需按时间顺序排列
     * @param monthsToForecast 预测月数，建议1-24个月
     * @return List<SaleRecordDTO> 预测结果的销售记录DTO列表
     * @throws IllegalArgumentException 当销售记录数据不足或格式不正确时抛出
     */
    public List<SaleRecordDTO> predictSalesWithARIMA(List<SaleRecord> saleRecords, int monthsToForecast) {
        
        validatePredictionParameters(saleRecords, monthsToForecast);

        List<Double> salesData = extractAndValidateSalesData(saleRecords);

        // 进行ARIMA预测
        ARIMAUtil.ARIMAResult arimaResult = ARIMAUtil.forecastCarSalesComplete(salesData, monthsToForecast);
        double[] forecastValues = arimaResult.getForecastValues();

        List<SaleRecordDTO> predictionResults = buildPredictionResults(saleRecords, forecastValues);

        return predictionResults;
    }

    /**
     * 基于销售记录进行ARIMA时间序列预测（带模型参数版本）
     * 
     * @param saleRecords      销售记录列表，需按时间顺序排列
     * @param monthsToForecast 预测月数，建议1-24个月
     * @param p                ARIMA模型的p参数
     * @param d                ARIMA模型的d参数
     * @param q                ARIMA模型的q参数
     * @return List<SaleRecordDTO> 预测结果的销售记录DTO列表
     * @throws IllegalArgumentException 当销售记录数据不足或格式不正确时抛出
     */
    public List<SaleRecordDTO> predictSalesWithARIMA(List<SaleRecord> saleRecords, int monthsToForecast, int p, int d, int q) {
        
        validatePredictionParameters(saleRecords, monthsToForecast);

        List<Double> salesData = extractAndValidateSalesData(saleRecords);

        // 进行ARIMA预测
        ARIMAUtil.ARIMAResult arimaResult = ARIMAUtil.forecastCarSalesWithParams(salesData, monthsToForecast, p, d, q);
        double[] forecastValues = arimaResult.getForecastValues();

        List<SaleRecordDTO> predictionResults = buildPredictionResults(saleRecords, forecastValues);

        return predictionResults;
    }

    private List<SaleRecordDTO> buildPredictionResults(List<SaleRecord> saleRecords, double[] forecastValues) {
        List<SaleRecordDTO> predictionResults = new ArrayList<>();

        // 获取最后一个销售记录作为基础数据
        SaleRecord lastRecord = saleRecords.get(saleRecords.size() - 1);

        // 为每个预测月份创建SaleRecordDTO
        for (int i = 0; i < forecastValues.length; i++) {
            double prediction = forecastValues[i];

            // 创建预测结果DTO
            SaleRecordDTO predictionDTO = new SaleRecordDTO();
            predictionDTO.setSaleId(null); // 预测记录没有ID
            predictionDTO.setCarModelId(lastRecord.getCarModelId());
            predictionDTO.setCarModelName(
                    lastRecord.getCarModel() != null ? lastRecord.getCarModel().getModelName() : "未知车型");
            predictionDTO.setRegionId(lastRecord.getRegionId());
            predictionDTO.setRegionName(
                    lastRecord.getRegion() != null ? lastRecord.getRegion().getRegionName() : "未知地区");
            predictionDTO.setSaleMonth(lastRecord.getSaleMonth().plusMonths(i + 1)); // 未来第i+1个月
            predictionDTO.setSaleCount((int) Math.round(prediction)); // 预测的销售数量
            predictionDTO.setSaleAmount(BigDecimal.valueOf(prediction * 50000)); // 估算销售金额

            predictionResults.add(predictionDTO);
        }
        return predictionResults;
    }

    private List<Double> extractAndValidateSalesData(List<SaleRecord> saleRecords) {
        // 提取销售数量数据并转换为Double列表
        List<Double> salesData = saleRecords.stream()
                .filter(record -> record.getSaleCount() != null) // 过滤空值
                .map(record -> record.getSaleCount().doubleValue()) // 转换为Double
                .collect(Collectors.toList());

        // 数据量验证
        if (salesData.size() < 10) {
            throw new IllegalArgumentException("有效销售数据不足10个点，无法进行ARIMA预测。当前数据点：" + salesData.size());
        }
        return salesData;
    }

    private void validatePredictionParameters(List<SaleRecord> saleRecords, int monthsToForecast) {
        // 参数验证
        if (saleRecords == null || saleRecords.isEmpty()) {
            throw new IllegalArgumentException("销售记录列表不能为空");
        }

        if (monthsToForecast <= 0 || monthsToForecast > 24) {
            throw new IllegalArgumentException("预测月数必须在1-24之间");
        }
    }
}
