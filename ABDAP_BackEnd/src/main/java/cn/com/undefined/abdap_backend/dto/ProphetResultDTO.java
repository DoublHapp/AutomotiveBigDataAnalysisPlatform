package cn.com.undefined.abdap_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Prophet预测结果数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProphetResultDTO {

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
     * 趋势组件
     */
    private double[] trendComponent;

    /**
     * 季节性组件
     */
    private double[] seasonalComponent;

    /**
     * 残差数组
     */
    private double[] residuals;

    /**
     * 平均绝对百分比误差 (%)
     */
    private double mape;

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

    // 新增：置信区间相关字段
    /**
     * 预测下界
     */
    private double[] forecastLower;

    /**
     * 预测上界
     */
    private double[] forecastUpper;

    /**
     * 趋势下界
     */
    private double[] trendLower;

    /**
     * 趋势上界
     */
    private double[] trendUpper;

    /**
     * 置信水平（如0.95表示95%置信区间）
     */
    private double confidenceLevel;

    /**
     * 预测区间宽度（置信上界 - 置信下界）
     */
    private double[] predictionIntervalWidth;

    /**
     * 从ProphetUtil.ProphetResult转换为DTO
     */
    public static ProphetResultDTO fromProphetResult(
            cn.com.undefined.abdap_backend.util.ProphetUtil.ProphetResult prophetResult) {
        ProphetResultDTO dto = new ProphetResultDTO();

        dto.setHistoricalData(prophetResult.getHistoricalData());
        dto.setFittedValues(prophetResult.getFittedValues());
        dto.setForecastValues(prophetResult.getForecastValues());
        dto.setTrendComponent(prophetResult.getTrendComponent());
        dto.setSeasonalComponent(prophetResult.getSeasonalComponent());
        dto.setResiduals(prophetResult.getResiduals());
        dto.setMape(prophetResult.getMape());
        dto.setForecastStartIndex(prophetResult.getForecastStartIndex());

        // 计算衍生字段
        dto.setCompleteTimeSeries(prophetResult.getCompleteTimeSeries());
        dto.setCompleteFittedSeries(prophetResult.getCompleteFittedSeries());
        dto.setHistoricalDataCount(prophetResult.getHistoricalData().length);
        dto.setForecastDataCount(prophetResult.getForecastValues().length);

        // 处理置信区间数据
        dto.setForecastLower(prophetResult.getForecastLower());
        dto.setForecastUpper(prophetResult.getForecastUpper());
        dto.setTrendLower(prophetResult.getTrendLower());
        dto.setTrendUpper(prophetResult.getTrendUpper());
        dto.setConfidenceLevel(prophetResult.getConfidenceLevel());

        // 计算预测区间宽度
        double[] forecastLower = prophetResult.getForecastLower();
        double[] forecastUpper = prophetResult.getForecastUpper();
        if (forecastLower != null && forecastUpper != null && forecastLower.length == forecastUpper.length) {
            double[] intervalWidth = new double[forecastLower.length];
            for (int i = 0; i < forecastLower.length; i++) {
                intervalWidth[i] = forecastUpper[i] - forecastLower[i];
            }
            dto.setPredictionIntervalWidth(intervalWidth);
        }

        return dto;
    }
}
