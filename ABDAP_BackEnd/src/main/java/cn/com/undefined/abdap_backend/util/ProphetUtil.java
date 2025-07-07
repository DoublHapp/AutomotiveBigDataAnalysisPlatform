package cn.com.undefined.abdap_backend.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Prophet时间序列分析工具类
 * 实现简化的Prophet预测功能
 * Prophet模型由三个主要组件组成：趋势、季节性和假日效应
 * 所有方法都是静态方法，可直接通过类名调用
 */
public class ProphetUtil {
    /**
     * Prophet预测结果包装类（扩展版本，支持置信区间）
     */
    @Getter
    public static class ProphetResult {
        private final double[] historicalData; // 原始历史数据
        private final double[] fittedValues; // 历史数据的拟合值
        private final double[] forecastValues; // 未来预测值
        private final double[] trendComponent; // 趋势组件
        private final double[] seasonalComponent; // 季节性组件
        private final double[] residuals; // 残差
        private final double mape; // 拟合误差
        private final int forecastStartIndex; // 预测开始的索引位置

        private final double[] forecastLower; // 预测下界
        private final double[] forecastUpper; // 预测上界
        private final double[] trendLower; // 趋势下界
        private final double[] trendUpper; // 趋势上界
        private final double confidenceLevel; // 置信水平

        public ProphetResult(double[] historicalData, double[] fittedValues,
                double[] forecastValues, double[] trendComponent,
                double[] seasonalComponent, double[] residuals, double mape,
                double[] forecastLower, double[] forecastUpper,
                double[] trendLower, double[] trendUpper, double confidenceLevel) {
            this.historicalData = historicalData;
            this.fittedValues = fittedValues;
            this.forecastValues = forecastValues;
            this.trendComponent = trendComponent;
            this.seasonalComponent = seasonalComponent;
            this.residuals = residuals;
            this.mape = mape;
            this.forecastStartIndex = historicalData.length;

            // 置信区间字段
            this.forecastLower = forecastLower;
            this.forecastUpper = forecastUpper;
            this.trendLower = trendLower;
            this.trendUpper = trendUpper;
            this.confidenceLevel = confidenceLevel;
        }

        /**
         * 获取完整的时间序列（历史+预测）
         */
        public double[] getCompleteTimeSeries() {
            double[] complete = new double[historicalData.length + forecastValues.length];
            System.arraycopy(historicalData, 0, complete, 0, historicalData.length);
            System.arraycopy(forecastValues, 0, complete, historicalData.length, forecastValues.length);
            return complete;
        }

        /**
         * 获取完整的拟合+预测序列
         */
        public double[] getCompleteFittedSeries() {
            double[] complete = new double[fittedValues.length + forecastValues.length];
            System.arraycopy(fittedValues, 0, complete, 0, fittedValues.length);
            System.arraycopy(forecastValues, 0, complete, fittedValues.length, forecastValues.length);
            return complete;
        }

        @Override
        public String toString() {
            return String.format("ProphetResult{历史数据点数=%d, 预测点数=%d, 拟合MAPE=%.2f%%}",
                    historicalData.length, forecastValues.length, mape);
        }
    }

    /**
     * Prophet模型配置类
     */
    @Getter
    @AllArgsConstructor
    public static class ProphetConfig {
        private final int seasonalityPeriod; // 季节性周期（默认12个月）
        private final double seasonalityStrength; // 季节性强度
        private final double trendFlexibility; // 趋势灵活性
        private final boolean includeHolidays; // 是否包含假日效应
        private final double confidenceLevel; // 置信水平（如0.95表示95%置信区间）

        public ProphetConfig() {
            this(12, 1.0, 0.05, false, 0.95);
        }
    }

    /**
     * 置信区间计算结果内部类
     */
    private static class ConfidenceIntervalResult {
        final double[] forecastLower;
        final double[] forecastUpper;
        final double[] trendLower;
        final double[] trendUpper;

        ConfidenceIntervalResult(double[] forecastLower, double[] forecastUpper,
                double[] trendLower, double[] trendUpper) {
            this.forecastLower = forecastLower;
            this.forecastUpper = forecastUpper;
            this.trendLower = trendLower;
            this.trendUpper = trendUpper;
        }
    }

    /**
     * 使用Prophet模型进行时间序列预测（支持置信区间）
     * 
     * @param data                      历史数据数组
     * @param forecastPeriods           预测期数
     * @param config                    Prophet配置
     * @param includeConfidenceInterval 是否计算置信区间
     * @param confidenceLevel           置信水平（如0.95表示95%置信区间）
     * @return 完整的Prophet预测结果（包含置信区间）
     */
    public static ProphetResult forecastWithProphet(double[] data, int forecastPeriods, ProphetConfig config) {
        if (data == null || data.length < 10) {
            throw new IllegalArgumentException("数据长度至少需要10个点");
        }

        try {
            // 1. 趋势组件分解
            double[] trendComponent = extractTrend(data, config.getTrendFlexibility());

            // 2. 季节性组件分解
            double[] seasonalComponent = extractSeasonality(data, trendComponent, config);

            // 3. 计算残差
            double[] residuals = calculateResiduals(data, trendComponent, seasonalComponent);

            // 4. 计算历史数据的拟合值
            double[] fittedValues = calculateFittedValues(trendComponent, seasonalComponent);

            // 5. 生成未来预测值
            double[] forecastValues = generateForecast(data, trendComponent, seasonalComponent,
                    forecastPeriods, config);

            // 6. 计算拟合误差
            double mape = calculateMAPE(data, fittedValues);

            // 7. 计算置信区间
            ConfidenceIntervalResult confidenceIntervals = calculateConfidenceIntervals(
                    data, trendComponent, seasonalComponent, residuals, forecastValues, config.getConfidenceLevel());

            return new ProphetResult(data, fittedValues, forecastValues,
                    trendComponent, seasonalComponent, residuals, mape,
                    confidenceIntervals.forecastLower, confidenceIntervals.forecastUpper,
                    confidenceIntervals.trendLower, confidenceIntervals.trendUpper,
                    config.getConfidenceLevel());

        } catch (Exception e) {
            throw new RuntimeException("Prophet预测失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用Prophet模型预测汽车销售数据
     * 
     * @param monthlySales     月度销售数据列表
     * @param monthsToForecast 预测的月份数
     * @param config           Prophet配置
     * @return ProphetResult 包含预测结果的对象
     */
    public static ProphetResult forecastCarSales(List<Double> monthlySales, int monthsToForecast,
            ProphetConfig config) {
        double[] data = monthlySales.stream().mapToDouble(Double::doubleValue).toArray();
        return forecastWithProphet(data, monthsToForecast, config);
    }

    public static ProphetResult forecastCarSales(List<Double> monthlySales, int monthsToForecast) {
        double[] data = monthlySales.stream().mapToDouble(Double::doubleValue).toArray();
        return forecastWithProphet(data, monthsToForecast, new ProphetConfig());
    }

    /**
     * 提取趋势组件（使用分段线性回归）
     */
    private static double[] extractTrend(double[] data, double flexibility) {
        double[] trend = new double[data.length];

        // 简化的趋势提取：使用移动平均和线性回归
        int windowSize = Math.max(3, data.length / 10);

        // 前半部分使用简单移动平均
        for (int i = 0; i < windowSize / 2; i++) {
            trend[i] = data[i];
        }

        // 中间部分使用移动平均
        for (int i = windowSize / 2; i < data.length - windowSize / 2; i++) {
            double sum = 0.0;
            for (int j = i - windowSize / 2; j <= i + windowSize / 2; j++) {
                sum += data[j];
            }
            trend[i] = sum / windowSize;
        }

        // 后半部分使用线性外推
        for (int i = data.length - windowSize / 2; i < data.length; i++) {
            if (i > 0) {
                double slope = (trend[i - 1] - trend[Math.max(0, i - 2)]) * flexibility;
                trend[i] = trend[i - 1] + slope;
            } else {
                trend[i] = data[i];
            }
        }

        return trend;
    }

    /**
     * 提取季节性组件（使用傅里叶级数）
     */
    private static double[] extractSeasonality(double[] data, double[] trend, ProphetConfig config) {
        double[] seasonal = new double[data.length];
        double[] detrended = new double[data.length];

        // 去趋势
        for (int i = 0; i < data.length; i++) {
            detrended[i] = data[i] - trend[i];
        }

        int period = config.getSeasonalityPeriod();
        double strength = config.getSeasonalityStrength();

        // 计算季节性模式（简化的傅里叶分析）
        for (int i = 0; i < data.length; i++) {
            double seasonalValue = 0.0;

            // 基础季节性（年度）
            double annualPhase = 2.0 * Math.PI * i / period;
            seasonalValue += strength * 0.5 * Math.sin(annualPhase);
            seasonalValue += strength * 0.3 * Math.cos(annualPhase);

            // 二次谐波（半年度）
            double semiAnnualPhase = 2.0 * Math.PI * i / (period / 2);
            seasonalValue += strength * 0.2 * Math.sin(semiAnnualPhase);

            // 使用实际数据调整季节性强度
            if (data.length >= period) {
                int seasonIndex = i % period;
                double seasonalAvg = calculateSeasonalAverage(detrended, seasonIndex, period);
                seasonalValue = seasonalValue * 0.3 + seasonalAvg * 0.7;
            }

            seasonal[i] = seasonalValue;
        }

        return seasonal;
    }

    /**
     * 计算特定季节位置的平均值
     */
    private static double calculateSeasonalAverage(double[] detrended, int seasonIndex, int period) {
        double sum = 0.0;
        int count = 0;

        for (int i = seasonIndex; i < detrended.length; i += period) {
            sum += detrended[i];
            count++;
        }

        return count > 0 ? sum / count : 0.0;
    }

    /**
     * 计算残差
     */
    private static double[] calculateResiduals(double[] data, double[] trend, double[] seasonal) {
        double[] residuals = new double[data.length];

        for (int i = 0; i < data.length; i++) {
            residuals[i] = data[i] - trend[i] - seasonal[i];
        }

        return residuals;
    }

    /**
     * 计算历史数据的拟合值
     */
    private static double[] calculateFittedValues(double[] trend, double[] seasonal) {
        double[] fitted = new double[trend.length];

        for (int i = 0; i < fitted.length; i++) {
            fitted[i] = trend[i] + seasonal[i];
        }

        return fitted;
    }

    /**
     * 生成未来预测值
     */
    private static double[] generateForecast(double[] data, double[] trend, double[] seasonal,
            int forecastPeriods, ProphetConfig config) {
        double[] forecast = new double[forecastPeriods];

        // 计算趋势的线性外推
        double trendSlope = calculateTrendSlope(trend);
        double lastTrend = trend[trend.length - 1];

        int period = config.getSeasonalityPeriod();
        double strength = config.getSeasonalityStrength();

        for (int i = 0; i < forecastPeriods; i++) {
            // 趋势组件
            double futureTrend = lastTrend + trendSlope * (i + 1);

            // 季节性组件（重复历史季节性模式）
            int seasonIndex = (data.length + i) % period;
            double futureSeasonal = 0.0;

            if (seasonIndex < seasonal.length) {
                futureSeasonal = seasonal[seasonIndex] * strength;
            } else {
                // 使用最近一个完整周期的季节性
                int recentSeasonIndex = seasonal.length - period + seasonIndex;
                if (recentSeasonIndex >= 0 && recentSeasonIndex < seasonal.length) {
                    futureSeasonal = seasonal[recentSeasonIndex] * strength;
                }
            }

            // 组合预测值
            forecast[i] = Math.max(0, futureTrend + futureSeasonal);
        }

        return forecast;
    }

    /**
     * 计算趋势斜率
     */
    private static double calculateTrendSlope(double[] trend) {
        if (trend.length < 2)
            return 0.0;

        // 使用最后几个点计算斜率
        int points = Math.min(6, trend.length);
        double sumX = 0.0, sumY = 0.0, sumXY = 0.0, sumX2 = 0.0;

        for (int i = 0; i < points; i++) {
            int idx = trend.length - points + i;
            double x = i;
            double y = trend[idx];
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = points * sumX2 - sumX * sumX;
        return denominator == 0 ? 0 : (points * sumXY - sumX * sumY) / denominator;
    }

    /**
     * 计算预测的准确性指标（MAPE - 平均绝对百分比误差）
     */
    public static double calculateMAPE(double[] actual, double[] predicted) {
        if (actual.length != predicted.length) {
            throw new IllegalArgumentException("实际值和预测值数组长度必须相同");
        }

        double mape = 0.0;
        int count = 0;

        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != 0) {
                mape += Math.abs((actual[i] - predicted[i]) / actual[i]);
                count++;
            }
        }
        return count > 0 ? (mape / count) * 100 : 0.0;
    }

    /**
     * 计算置信区间
     * 基于历史残差的分布来估计未来预测的不确定性
     * 
     * @param data            历史数据
     * @param trend           趋势组件
     * @param seasonal        季节性组件
     * @param residuals       历史残差
     * @param forecastValues  预测值
     * @param confidenceLevel 置信水平
     * @return 置信区间结果
     */
    private static ConfidenceIntervalResult calculateConfidenceIntervals(
            double[] data, double[] trend, double[] seasonal, double[] residuals,
            double[] forecastValues, double confidenceLevel) {

        // 计算残差的标准差
        double residualStd = calculateStandardDeviation(residuals);

        // 计算趋势不确定性（基于趋势变化的方差）
        double trendUncertainty = calculateTrendUncertainty(trend);

        // 根据置信水平计算Z值（正态分布分位数的近似）
        double zValue = calculateZValue(confidenceLevel);

        // 预测置信区间
        double[] forecastLower = new double[forecastValues.length];
        double[] forecastUpper = new double[forecastValues.length];

        // 趋势置信区间
        double[] trendLower = new double[forecastValues.length];
        double[] trendUpper = new double[forecastValues.length];

        // 计算未来趋势值（简化估算）
        double lastTrend = trend[trend.length - 1];
        double trendSlope = calculateTrendSlope(trend);

        for (int i = 0; i < forecastValues.length; i++) {
            // 预测不确定性随时间增加
            double timeMultiplier = 1.0 + (i * 0.1); // 时间越远，不确定性越大
            double predictionStd = residualStd * timeMultiplier;

            // 预测置信区间
            double margin = zValue * predictionStd;
            forecastLower[i] = forecastValues[i] - margin;
            forecastUpper[i] = forecastValues[i] + margin;

            // 趋势置信区间
            double futureTrend = lastTrend + trendSlope * (i + 1);
            double trendMargin = zValue * trendUncertainty * timeMultiplier;
            trendLower[i] = futureTrend - trendMargin;
            trendUpper[i] = futureTrend + trendMargin;
        }

        return new ConfidenceIntervalResult(forecastLower, forecastUpper, trendLower, trendUpper);
    }

    /**
     * 计算标准差
     */
    private static double calculateStandardDeviation(double[] values) {
        if (values.length == 0)
            return 0.0;

        double mean = Arrays.stream(values).average().orElse(0.0);
        double variance = 0.0;

        for (double value : values) {
            variance += Math.pow(value - mean, 2);
        }

        return Math.sqrt(variance / (values.length - 1));
    }

    /**
     * 计算趋势不确定性
     */
    private static double calculateTrendUncertainty(double[] trend) {
        if (trend.length < 3)
            return 0.1;

        // 计算趋势变化的方差
        double[] trendChanges = new double[trend.length - 1];
        for (int i = 1; i < trend.length; i++) {
            trendChanges[i - 1] = trend[i] - trend[i - 1];
        }

        return calculateStandardDeviation(trendChanges);
    }

    /**
     * 根据置信水平计算Z值（正态分布分位数近似）
     */
    private static double calculateZValue(double confidenceLevel) {
        // 常用置信水平的Z值
        if (confidenceLevel >= 0.99)
            return 2.576;
        if (confidenceLevel >= 0.95)
            return 1.960;
        if (confidenceLevel >= 0.90)
            return 1.645;
        if (confidenceLevel >= 0.80)
            return 1.282;

        // 简化的近似公式
        double alpha = 1.0 - confidenceLevel;
        return Math.sqrt(-2.0 * Math.log(alpha / 2.0));
    }

    /**
     * 创建自定义Prophet配置
     * 
     * @param seasonalityPeriod   季节性周期
     * @param seasonalityStrength 季节性强度 (0.0-1.0)
     * @param trendFlexibility    趋势灵活性 (0.0-1.0)
     * @param includeHolidays     是否包含假日效应
     * @param confidenceLevel     置信水平（如0.95表示95%置信区间）
     * @return Prophet配置对象
     */
    public static ProphetConfig createCustomConfig(int seasonalityPeriod, double seasonalityStrength,
            double trendFlexibility, boolean includeHolidays, double confidenceLevel) {
        return new ProphetConfig(seasonalityPeriod, seasonalityStrength, trendFlexibility, includeHolidays,
                confidenceLevel);
    }

    public static ProphetConfig createCustomConfig(int seasonalityPeriod, double seasonalityStrength,
            double trendFlexibility, boolean includeHolidays) {
        return new ProphetConfig(seasonalityPeriod, seasonalityStrength, trendFlexibility, includeHolidays,
                0.95);
    }

    /**
     * 模型诊断：检查残差的统计特性
     * 
     * @param residuals 残差数组
     * @return 诊断结果字符串
     */
    public static String diagnosisModel(double[] residuals) {
        if (residuals == null || residuals.length == 0) {
            return "无法进行模型诊断：残差数据为空";
        }

        // 计算残差统计量
        double mean = Arrays.stream(residuals).average().orElse(0.0);
        double variance = Arrays.stream(residuals)
                .map(x -> (x - mean) * (x - mean))
                .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        // 计算自相关
        double autocorr = calculateAutocorrelation(residuals, 1);

        StringBuilder diagnosis = new StringBuilder();
        diagnosis.append("Prophet模型诊断报告:\n");
        diagnosis.append(String.format("残差均值: %.4f (接近0表示无偏)\n", mean));
        diagnosis.append(String.format("残差标准差: %.4f\n", stdDev));
        diagnosis.append(String.format("一阶自相关: %.4f (接近0表示无序列相关)\n", autocorr));

        // 简单的诊断结论
        if (Math.abs(mean) < 0.1 * stdDev) {
            diagnosis.append("✓ 残差均值接近0，模型无明显偏差\n");
        } else {
            diagnosis.append("⚠ 残差均值偏离0，可能存在系统性偏差\n");
        }

        if (Math.abs(autocorr) < 0.2) {
            diagnosis.append("✓ 残差自相关性较低，模型拟合良好\n");
        } else {
            diagnosis.append("⚠ 残差存在自相关性，可能需要调整模型参数\n");
        }

        return diagnosis.toString();
    }

    /**
     * 计算自相关系数
     */
    private static double calculateAutocorrelation(double[] data, int lag) {
        if (data.length <= lag)
            return 0.0;

        double mean = Arrays.stream(data).average().orElse(0.0);
        double variance = Arrays.stream(data)
                .map(x -> (x - mean) * (x - mean))
                .sum();

        if (variance == 0)
            return 0.0;

        double covariance = 0.0;
        for (int i = 0; i < data.length - lag; i++) {
            covariance += (data[i] - mean) * (data[i + lag] - mean);
        }

        return covariance / variance;
    }
}
