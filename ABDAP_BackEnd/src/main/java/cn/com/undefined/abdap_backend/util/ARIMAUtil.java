package cn.com.undefined.abdap_backend.util;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * ARIMA时间序列分析工具类
 * 实现简洁的ARIMA预测功能
 * 所有方法都是静态方法，可直接通过类名调用
 */
public class ARIMAUtil {
    /**
     * ARIMA预测结果包装类
     */
    @Getter
    public static class ARIMAResult {
        private final double[] historicalData; // 原始历史数据
        private final double[] fittedValues; // 历史数据的拟合值
        private final double[] forecastValues; // 未来预测值
        private final double[] forecastUpperBounds; // 预测值上界
        private final double[] forecastLowerBounds; // 预测值下界
        private final double[] residuals; // 残差
        private final double mape; // 拟合误差
        private final double confidenceLevel; // 置信度水平
        private final int forecastStartIndex; // 预测开始的索引位置

        public ARIMAResult(double[] historicalData, double[] fittedValues,
                double[] forecastValues, double[] forecastUpperBounds, double[] forecastLowerBounds,
                double[] residuals, double mape, double confidenceLevel) {
            this.historicalData = historicalData;
            this.fittedValues = fittedValues;
            this.forecastValues = forecastValues;
            this.forecastUpperBounds = forecastUpperBounds;
            this.forecastLowerBounds = forecastLowerBounds;
            this.residuals = residuals;
            this.mape = mape;
            this.confidenceLevel = confidenceLevel;
            this.forecastStartIndex = historicalData.length;
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
            return String.format("ARIMAResult{历史数据点数=%d, 预测点数=%d, 拟合MAPE=%.2f%%, 置信度=%.1f%%}",
                    historicalData.length, forecastValues.length, mape, confidenceLevel * 100);
        }
    }

    /**
     * 计算预测的置信区间
     * 
     * @param residuals       残差数组
     * @param forecastPeriods 预测期数
     * @param confidenceLevel 置信度水平 (例如0.95表示95%置信度)
     * @return 置信区间的标准误差倍数
     */
    private static double calculateConfidenceMultiplier(double[] residuals, int forecastPeriods,
            double confidenceLevel) {
        // 计算残差标准差
        double residualStd = calculateStandardDeviation(residuals);

        // 根据置信度计算z值 (简化处理，使用正态分布近似)
        double alpha = 1.0 - confidenceLevel;
        double zValue = calculateZValue(alpha / 2.0);

        return zValue * residualStd;
    }

    /**
     * 计算标准差
     */
    private static double calculateStandardDeviation(double[] data) {
        if (data.length <= 1)
            return 0.0;

        double mean = Arrays.stream(data).average().orElse(0.0);
        double variance = Arrays.stream(data)
                .map(x -> Math.pow(x - mean, 2))
                .average().orElse(0.0);

        return Math.sqrt(variance);
    }

    /**
     * 计算Z值 (正态分布分位数的简化近似)
     */
    private static double calculateZValue(double alpha) {
        // 常用置信度的Z值映射 (简化处理)
        if (alpha <= 0.005)
            return 2.576; // 99%置信度
        if (alpha <= 0.025)
            return 1.96; // 95%置信度
        if (alpha <= 0.05)
            return 1.645; // 90%置信度
        return 1.0; // 默认值
    }

    /**
     * 计算预测置信区间
     */
    private static double[][] calculateForecastConfidenceIntervals(double[] forecastValues,
            double[] residuals, double confidenceLevel) {
        double multiplier = calculateConfidenceMultiplier(residuals, forecastValues.length, confidenceLevel);

        double[] upperBounds = new double[forecastValues.length];
        double[] lowerBounds = new double[forecastValues.length];

        for (int i = 0; i < forecastValues.length; i++) {
            // 随着预测步数增加，不确定性增大
            double stepMultiplier = multiplier * Math.sqrt(i + 1);
            upperBounds[i] = Math.max(0, forecastValues[i] + stepMultiplier);
            lowerBounds[i] = Math.max(0, forecastValues[i] - stepMultiplier);
        }

        return new double[][] { upperBounds, lowerBounds };
    }

    /**
     * 使用ARIMA模型进行时间序列预测
     * 
     * @param data            历史数据数组
     * @param p               自回归阶数
     * @param d               差分阶数
     * @param q               移动平均阶数
     * @param forecastPeriods 预测期数
     * @return 预测结果数组
     */
    public static double[] forecastWithARIMA(double[] data, int p, int d, int q, int forecastPeriods) {
        if (data == null || data.length < 10) {
            throw new IllegalArgumentException("数据长度至少需要10个点");
        }

        try {
            // 数据预处理 - 差分处理
            double[] processedData = applyDifferencing(data, d);

            // 简化的ARIMA预测实现
            return simpleARIMAForecast(processedData, data, forecastPeriods, p, q);

        } catch (Exception e) {
            throw new RuntimeException("ARIMA预测失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用ARIMA模型进行时间序列预测（返回完整结果）
     * 
     * @param data            历史数据数组
     * @param p               自回归阶数
     * @param d               差分阶数
     * @param q               移动平均阶数
     * @param forecastPeriods 预测期数
     * @return 完整的ARIMA预测结果
     */
    public static ARIMAResult forecastWithARIMAComplete(double[] data, int p, int d, int q, int forecastPeriods) {
        return forecastWithARIMAComplete(data, p, d, q, forecastPeriods, 0.95);
    }

    /**
     * 使用ARIMA模型进行时间序列预测（返回完整结果，带置信度）
     * 
     * @param data            历史数据数组
     * @param p               自回归阶数
     * @param d               差分阶数
     * @param q               移动平均阶数
     * @param forecastPeriods 预测期数
     * @param confidenceLevel 置信度水平 (例如0.95表示95%置信度)
     * @return 完整的ARIMA预测结果
     */
    public static ARIMAResult forecastWithARIMAComplete(double[] data, int p, int d, int q,
            int forecastPeriods, double confidenceLevel) {
        if (data == null || data.length < 10) {
            throw new IllegalArgumentException("数据长度至少需要10个点");
        }

        try {
            // 数据预处理 - 差分处理
            double[] processedData = applyDifferencing(data, d);

            // 计算模型参数
            double[] arCoeffs = calculateARCoefficients(processedData, p);
            double[] maCoeffs = calculateMACoefficients(processedData, q);
            double[] residuals = calculateResiduals(processedData, arCoeffs, maCoeffs);

            // 计算历史数据的拟合值
            double[] fittedValues = calculateFittedValues(data, processedData, arCoeffs, maCoeffs, d);

            // 生成未来预测值
            double[] forecastValues = generateForecast(processedData, data, forecastPeriods, arCoeffs, maCoeffs);

            // 计算预测置信区间
            double[][] confidenceIntervals = calculateForecastConfidenceIntervals(forecastValues, residuals,
                    confidenceLevel);
            double[] upperBounds = confidenceIntervals[0];
            double[] lowerBounds = confidenceIntervals[1];

            // 计算拟合误差
            double mape = calculateMAPE(data, fittedValues);

            return new ARIMAResult(data, fittedValues, forecastValues, upperBounds, lowerBounds,
                    residuals, mape, confidenceLevel);

        } catch (Exception e) {
            throw new RuntimeException("ARIMA预测失败: " + e.getMessage(), e);
        }
    }

    /**
     * 应用差分操作
     */
    private static double[] applyDifferencing(double[] data, int d) {
        double[] result = Arrays.copyOf(data, data.length);

        for (int diff = 0; diff < d; diff++) {
            double[] temp = new double[result.length - 1];
            for (int i = 1; i < result.length; i++) {
                temp[i - 1] = result[i] - result[i - 1];
            }
            result = temp;
        }

        return result;
    }

    /**
     * 简化的ARIMA预测实现
     * 结合自回归和移动平均
     */
    private static double[] simpleARIMAForecast(double[] diffData, double[] originalData,
            int forecastPeriods, int p, int q) {
        double[] forecast = new double[forecastPeriods];

        // 计算自回归系数
        double[] arCoeffs = calculateARCoefficients(diffData, p);

        // 计算移动平均系数
        double[] maCoeffs = calculateMACoefficients(diffData, q);

        // 计算残差
        double[] residuals = calculateResiduals(diffData, arCoeffs, maCoeffs);

        // 生成预测值
        for (int i = 0; i < forecastPeriods; i++) {
            double arTerm = 0.0;
            double maTerm = 0.0;

            // 自回归项
            for (int j = 0; j < p && j < diffData.length; j++) {
                int idx = diffData.length - 1 - j;
                if (idx >= 0) {
                    arTerm += arCoeffs[j] * diffData[idx];
                }
            }

            // 移动平均项
            for (int j = 0; j < q && j < residuals.length; j++) {
                int idx = residuals.length - 1 - j;
                if (idx >= 0) {
                    maTerm += maCoeffs[j] * residuals[idx];
                }
            }

            forecast[i] = arTerm + maTerm;
        }

        // 还原差分
        return restoreDifferencing(forecast, originalData);
    }

    /**
     * 计算自回归系数（简化版本）
     */
    private static double[] calculateARCoefficients(double[] data, int p) {
        double[] coeffs = new double[p];

        if (data.length < p + 1) {
            Arrays.fill(coeffs, 0.1); // 默认小系数
            return coeffs;
        }

        // 使用最小二乘法计算系数
        for (int i = 0; i < p; i++) {
            double correlation = calculateCorrelation(data, i + 1);
            coeffs[i] = correlation * 0.5; // 简化处理
        }

        return coeffs;
    }

    /**
     * 计算移动平均系数
     */
    private static double[] calculateMACoefficients(double[] data, int q) {
        double[] coeffs = new double[q];
        Arrays.fill(coeffs, 0.3); // 简化处理，使用固定系数
        return coeffs;
    }

    /**
     * 计算残差
     */
    private static double[] calculateResiduals(double[] data, double[] arCoeffs, double[] maCoeffs) {
        double[] residuals = new double[data.length];

        for (int i = 0; i < data.length; i++) {
            double predicted = 0.0;

            // 自回归项
            for (int j = 0; j < arCoeffs.length && i - j - 1 >= 0; j++) {
                predicted += arCoeffs[j] * data[i - j - 1];
            }

            residuals[i] = data[i] - predicted;
        }

        return residuals;
    }

    /**
     * 计算相关系数
     */
    private static double calculateCorrelation(double[] data, int lag) {
        if (data.length <= lag)
            return 0.0;

        double sum1 = 0.0, sum2 = 0.0, sum1Sq = 0.0, sum2Sq = 0.0, pSum = 0.0;
        int n = data.length - lag;

        for (int i = 0; i < n; i++) {
            sum1 += data[i];
            sum2 += data[i + lag];
            sum1Sq += data[i] * data[i];
            sum2Sq += data[i + lag] * data[i + lag];
            pSum += data[i] * data[i + lag];
        }

        double num = pSum - (sum1 * sum2 / n);
        double den = Math.sqrt((sum1Sq - sum1 * sum1 / n) * (sum2Sq - sum2 * sum2 / n));

        return den == 0 ? 0 : num / den;
    }

    /**
     * 还原差分操作
     */
    private static double[] restoreDifferencing(double[] forecast, double[] originalData) {
        double[] result = Arrays.copyOf(forecast, forecast.length);

        // 简化处理：基于最后一个原始值进行累加
        double lastValue = originalData[originalData.length - 1];

        for (int i = 0; i < result.length; i++) {
            result[i] = lastValue + result[i];
            lastValue = result[i];
        }

        return result;
    }

    /**
     * 自动确定ARIMA参数并预测
     * 
     * @param data            历史数据
     * @param forecastPeriods 预测期数
     * @return 预测结果
     */
    public static double[] autoForecast(double[] data, int forecastPeriods) {
        // 自动选择参数 (简化版本)
        int p = 2; // 自回归阶数
        int d = 1; // 差分阶数
        int q = 1; // 移动平均阶数

        return forecastWithARIMA(data, p, d, q, forecastPeriods);
    }

    /**
     * 预测汽车销量（专用方法）
     * 
     * @param monthlySales     月度销量数据
     * @param monthsToForecast 预测月数
     * @return 预测的月度销量
     */
    public static double[] forecastCarSales(List<Double> monthlySales, int monthsToForecast) {
        double[] data = monthlySales.stream().mapToDouble(Double::doubleValue).toArray();
        return autoForecast(data, monthsToForecast);
    }

    /**
     * 简单的移动平均预测（备用方法）
     * 
     * @param data       历史数据
     * @param periods    预测期数
     * @param windowSize 移动平均窗口大小
     * @return 预测结果
     */
    public static double[] movingAverageWithTrend(double[] data, int periods, int windowSize) {
        if (data.length < windowSize) {
            throw new IllegalArgumentException("数据长度不足");
        }

        double[] forecast = new double[periods];

        // 计算移动平均
        double sum = Arrays.stream(data, data.length - windowSize, data.length).sum();
        double avg = sum / windowSize;

        // 计算趋势
        double trend = calculateLinearTrend(data, windowSize);

        // 生成预测
        for (int i = 0; i < periods; i++) {
            forecast[i] = Math.max(0, avg + trend * (i + 1));
        }

        return forecast;
    }

    /**
     * 计算线性趋势
     */
    private static double calculateLinearTrend(double[] data, int windowSize) {
        int startIdx = Math.max(0, data.length - windowSize);
        int n = data.length - startIdx;

        if (n < 2)
            return 0.0;

        double sumX = IntStream.range(0, n).sum();
        double sumY = Arrays.stream(data, startIdx, data.length).sum();
        double sumXY = IntStream.range(0, n)
                .mapToDouble(i -> i * data[startIdx + i])
                .sum();
        double sumX2 = IntStream.range(0, n)
                .mapToDouble(i -> i * i)
                .sum();

        double denominator = n * sumX2 - sumX * sumX;
        return denominator == 0 ? 0 : (n * sumXY - sumX * sumY) / denominator;
    }

    /**
     * 计算预测的准确性指标（MAPE - 平均绝对百分比误差）
     * 
     * @param actual    实际值
     * @param predicted 预测值
     * @return MAPE值
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
     * 计算历史数据的拟合值
     */
    private static double[] calculateFittedValues(double[] originalData, double[] diffData,
            double[] arCoeffs, double[] maCoeffs, int d) {
        double[] fitted = new double[originalData.length];

        // 前几个点使用原始值（因为差分和模型限制）
        int startIdx = Math.max(d, Math.max(arCoeffs.length, maCoeffs.length));
        for (int i = 0; i < startIdx && i < fitted.length; i++) {
            fitted[i] = originalData[i];
        }

        // 从有足够历史数据的点开始计算拟合值
        for (int i = startIdx; i < fitted.length; i++) {
            double fittedDiff = 0.0;

            // 自回归项
            for (int j = 0; j < arCoeffs.length && i - j - 1 >= 0; j++) {
                if (i - j - 1 - d >= 0 && i - j - 1 - d < diffData.length) {
                    fittedDiff += arCoeffs[j] * diffData[i - j - 1 - d];
                }
            }

            // 移动平均项（简化处理）
            for (int j = 0; j < maCoeffs.length; j++) {
                fittedDiff += maCoeffs[j] * 0.1; // 简化处理
            }

            // 还原差分得到拟合值
            fitted[i] = originalData[i - 1] + fittedDiff;
        }

        return fitted;
    }

    /**
     * 生成未来预测值
     */
    private static double[] generateForecast(double[] diffData, double[] originalData,
            int forecastPeriods, double[] arCoeffs, double[] maCoeffs) {
        double[] forecast = new double[forecastPeriods];

        // 生成预测值
        for (int i = 0; i < forecastPeriods; i++) {
            double arTerm = 0.0;
            double maTerm = 0.0;

            // 自回归项
            for (int j = 0; j < arCoeffs.length && j < diffData.length; j++) {
                int idx = diffData.length - 1 - j;
                if (idx >= 0) {
                    arTerm += arCoeffs[j] * diffData[idx];
                }
            }

            // 移动平均项
            for (int j = 0; j < maCoeffs.length; j++) {
                maTerm += maCoeffs[j] * 0.1; // 简化处理
            }

            forecast[i] = arTerm + maTerm;
        }

        // 还原差分
        return restoreDifferencing(forecast, originalData);
    }

    /**
     * 自动预测并返回完整结果（包含拟合值）
     * 
     * @param data            历史数据
     * @param forecastPeriods 预测期数
     * @return 完整预测结果
     */
    public static ARIMAResult autoForecastComplete(double[] data, int forecastPeriods) {
        return autoForecastComplete(data, forecastPeriods, 0.95);
    }

    /**
     * 自动预测并返回完整结果（包含拟合值和置信区间）
     * 
     * @param data            历史数据
     * @param forecastPeriods 预测期数
     * @param confidenceLevel 置信度水平
     * @return 完整预测结果
     */
    public static ARIMAResult autoForecastComplete(double[] data, int forecastPeriods, double confidenceLevel) {
        // 自动选择参数 (简化版本)
        int p = 2; // 自回归阶数
        int d = 1; // 差分阶数
        int q = 1; // 移动平均阶数

        return forecastWithARIMAComplete(data, p, d, q, forecastPeriods, confidenceLevel);
    }

    /**
     * 预测汽车销量（完整结果版本）
     * 
     * @param monthlySales     月度销量数据
     * @param monthsToForecast 预测月数
     * @return 完整预测结果
     */
    public static ARIMAResult forecastCarSalesComplete(List<Double> monthlySales, int monthsToForecast) {
        return forecastCarSalesComplete(monthlySales, monthsToForecast, 0.95);
    }

    /**
     * 预测汽车销量（完整结果版本，带置信度）
     * 
     * @param monthlySales     月度销量数据
     * @param monthsToForecast 预测月数
     * @param confidenceLevel  置信度水平
     * @return 完整预测结果
     */
    public static ARIMAResult forecastCarSalesComplete(List<Double> monthlySales, int monthsToForecast,
            double confidenceLevel) {
        double[] data = monthlySales.stream().mapToDouble(Double::doubleValue).toArray();
        return autoForecastComplete(data, monthsToForecast, confidenceLevel);
    }

    /**
     * 预测汽车销量（手动配置参数版本）
     * 
     * @param monthlySales     月度销量数据
     * @param monthsToForecast 预测月数
     * @param p                自回归阶数
     * @param d                差分阶数
     * @param q                移动平均阶数
     * @return 完整预测结果
     */
    public static ARIMAResult forecastCarSalesComplete(List<Double> monthlySales, int monthsToForecast,
            int p, int d, int q) {
        return forecastCarSalesComplete(monthlySales, monthsToForecast, p, d, q, 0.95);
    }

    /**
     * 预测汽车销量（手动配置参数版本，带置信度）
     * 
     * @param monthlySales     月度销量数据
     * @param monthsToForecast 预测月数
     * @param p                自回归阶数
     * @param d                差分阶数
     * @param q                移动平均阶数
     * @param confidenceLevel  置信度水平
     * @return 完整预测结果
     */
    public static ARIMAResult forecastCarSalesComplete(List<Double> monthlySales, int monthsToForecast,
            int p, int d, int q, double confidenceLevel) {
        double[] data = monthlySales.stream().mapToDouble(Double::doubleValue).toArray();
        return forecastWithARIMAComplete(data, p, d, q, monthsToForecast, confidenceLevel);
    }
}
