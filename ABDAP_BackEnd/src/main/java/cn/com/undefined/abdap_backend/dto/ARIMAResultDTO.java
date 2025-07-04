package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * ARIMA预测结果数据传输对象
 * 纯数据DTO，用于前后端数据交互
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ARIMAResultDTO {

    /**
     * 原始历史数据
     */
    private double[] historicalData;

    /**
     * 历史数据的拟合值
     */
    private double[] fittedValues;
    /**
     * 未来预测值
     */
    private double[] forecastValues;

    /**
     * 预测值上界
     */
    private double[] forecastUpperBounds;

    /**
     * 预测值下界
     */
    private double[] forecastLowerBounds;

    /**
     * 残差数组
     */
    private double[] residuals;
    /**
     * 平均绝对百分比误差 (%)
     */
    private double mape;

    /**
     * 置信度水平
     */
    private double confidenceLevel;

    /**
     * 预测开始的索引位置
     */
    private int forecastStartIndex;

    /**
     * 完整的时间序列（历史+预测）
     */
    private double[] completeTimeSeries;

    /**
     * 完整的拟合+预测序列
     */
    private double[] completeFittedSeries;

    /**
     * 历史数据点数
     */
    private int historicalDataCount;

    /**
     * 预测数据点数
     */
    private int forecastDataCount;

    /**
     * 从ARIMAUtil.ARIMAResult转换为DTO
     * 
     * @param arimaResult ARIMA预测结果
     * @return ARIMAResultDTO
     */
    public static ARIMAResultDTO fromARIMAResult(
            cn.com.undefined.abdap_backend.util.ARIMAUtil.ARIMAResult arimaResult) {
        ARIMAResultDTO dto = new ARIMAResultDTO();
        dto.setHistoricalData(arimaResult.getHistoricalData());
        dto.setFittedValues(arimaResult.getFittedValues());
        dto.setForecastValues(arimaResult.getForecastValues());
        dto.setForecastUpperBounds(arimaResult.getForecastUpperBounds());
        dto.setForecastLowerBounds(arimaResult.getForecastLowerBounds());
        dto.setResiduals(arimaResult.getResiduals());
        dto.setMape(arimaResult.getMape());
        dto.setConfidenceLevel(arimaResult.getConfidenceLevel());
        dto.setForecastStartIndex(arimaResult.getForecastStartIndex());

        // 计算衍生字段
        dto.setCompleteTimeSeries(arimaResult.getCompleteTimeSeries());
        dto.setCompleteFittedSeries(arimaResult.getCompleteFittedSeries());
        dto.setHistoricalDataCount(arimaResult.getHistoricalData().length);
        dto.setForecastDataCount(arimaResult.getForecastValues().length);

        return dto;
    }

}
