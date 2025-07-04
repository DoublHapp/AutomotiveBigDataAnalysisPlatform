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
     * Prophet预测结果包装类
     */
    public static class ProphetResult {
        private final double[] historicalData; // 原始历史数据
        private final double[] fittedValues; // 历史数据的拟合值
        private final double[] forecastValues; // 未来预测值
        private final double[] trendComponent; // 趋势组件
        private final double[] seasonalComponent; // 季节性组件
        private final double[] residuals; // 残差
        private final double mape; // 拟合误差
        private final int forecastStartIndex; // 预测开始的索引位置

        public ProphetResult(double[] historicalData, double[] fittedValues,
                double[] forecastValues, double[] trendComponent,
                double[] seasonalComponent, double[] residuals, double mape) {
            this.historicalData = historicalData;
            this.fittedValues = fittedValues;
            this.forecastValues = forecastValues;
            this.trendComponent = trendComponent;
            this.seasonalComponent = seasonalComponent;
            this.residuals = residuals;
            this.mape = mape;
            this.forecastStartIndex = historicalData.length;
        }

        // Getters
        public double[] getHistoricalData() {
            return historicalData;
        }

        public double[] getFittedValues() {
            return fittedValues;
        }

        public double[] getForecastValues() {
            return forecastValues;
        }

        public double[] getTrendComponent() {
            return trendComponent;
        }

        public double[] getSeasonalComponent() {
            return seasonalComponent;
        }

        public double[] getResiduals() {
            return residuals;
        }

        public double getMAPE() {
            return mape;
        }

        public int getForecastStartIndex() {
            return forecastStartIndex;
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

        public ProphetConfig() {
            this(12, 1.0, 0.05, false);
        }
    }

    /**
     * 使用Prophet模型进行时间序列预测
     * 
     * @param data            历史数据数组
     * @param forecastPeriods 预测期数
     * @param config          Prophet配置
     * @return 完整的Prophet预测结果
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

            return new ProphetResult(data, fittedValues, forecastValues,
                    trendComponent, seasonalComponent, residuals, mape);

        } catch (Exception e) {
            throw new RuntimeException("Prophet预测失败: " + e.getMessage(), e);
        }
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
     * 自动Prophet预测（使用默认配置）
     * 
     * @param data            历史数据
     * @param forecastPeriods 预测期数
     * @return 预测结果
     */
    public static ProphetResult autoForecast(double[] data, int forecastPeriods) {
        ProphetConfig config = new ProphetConfig(); // 使用默认配置
        return forecastWithProphet(data, forecastPeriods, config);
    }

    /**
     * 预测汽车销量（专用方法）
     * 适用于月度汽车销量数据，考虑年度季节性
     * 
     * @param monthlySales     月度销量数据
     * @param monthsToForecast 预测月数
     * @return 预测结果
     */
    public static ProphetResult forecastCarSales(List<Double> monthlySales, int monthsToForecast) {
        double[] data = monthlySales.stream().mapToDouble(Double::doubleValue).toArray();

        // 汽车销量专用配置：12个月季节性，中等季节性强度
        ProphetConfig config = new ProphetConfig(12, 0.8, 0.1, false);

        return forecastWithProphet(data, monthsToForecast, config);
    }

    /**
     * 预测季度销量（专用方法）
     * 
     * @param quarterlySales     季度销量数据
     * @param quartersToForecast 预测季度数
     * @return 预测结果
     */
    public static ProphetResult forecastQuarterlySales(List<Double> quarterlySales, int quartersToForecast) {
        double[] data = quarterlySales.stream().mapToDouble(Double::doubleValue).toArray();

        // 季度数据配置：4个季度季节性
        ProphetConfig config = new ProphetConfig(4, 0.6, 0.08, false);

        return forecastWithProphet(data, quartersToForecast, config);
    }

    /**
     * 创建自定义Prophet配置
     * 
     * @param seasonalityPeriod   季节性周期
     * @param seasonalityStrength 季节性强度 (0.0-1.0)
     * @param trendFlexibility    趋势灵活性 (0.0-1.0)
     * @param includeHolidays     是否包含假日效应
     * @return Prophet配置对象
     */
    public static ProphetConfig createCustomConfig(int seasonalityPeriod, double seasonalityStrength,
            double trendFlexibility, boolean includeHolidays) {
        return new ProphetConfig(seasonalityPeriod, seasonalityStrength, trendFlexibility, includeHolidays);
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
