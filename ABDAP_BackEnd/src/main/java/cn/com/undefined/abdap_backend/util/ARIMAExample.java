package cn.com.undefined.abdap_backend.util;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

/**
 * ARIMA工具类使用示例
 */
@Service
public class ARIMAExample {
    
    /**
     * 汽车销量预测示例
     */
    public void demonstrateCarSalesForecast() {
        // 模拟12个月的汽车销量数据（单位：万辆）
        List<Double> monthlySales = Arrays.asList(
            15.2, 18.5, 22.1, 19.8, 25.3, 28.7,
            32.1, 29.4, 26.8, 23.5, 20.1, 17.6
        );
        
        // 预测未来6个月的销量
        double[] forecast = ARIMAUtil.forecastCarSales(monthlySales, 6);
        
        System.out.println("历史销量数据: " + monthlySales);
        System.out.println("预测未来6个月销量: " + Arrays.toString(forecast));
    }
    
    /**
     * 自定义ARIMA参数预测示例
     */
    public void demonstrateCustomARIMA() {
        // 历史数据
        double[] data = {100, 105, 110, 108, 115, 120, 118, 125, 130, 128, 135, 140};
        
        // 使用ARIMA(2,1,1)模型预测未来5个点
        double[] forecast = ARIMAUtil.forecastWithARIMA(data, 2, 1, 1, 5);
        
        System.out.println("历史数据: " + Arrays.toString(data));
        System.out.println("ARIMA(2,1,1)预测结果: " + Arrays.toString(forecast));
    }
    
    /**
     * 预测准确性评估示例
     */
    public void demonstrateAccuracyEvaluation() {
        double[] actual = {120, 125, 130, 135, 140};
        double[] predicted = {118, 127, 132, 133, 142};
        
        double mape = ARIMAUtil.calculateMAPE(actual, predicted);
        System.out.println("预测准确性 (MAPE): " + String.format("%.2f%%", mape));
    }
    
    /**
     * 移动平均趋势预测示例
     */
    public void demonstrateMovingAverageForecast() {
        double[] data = {50, 55, 60, 58, 65, 70, 68, 75, 80, 78, 85, 90};
        
        // 使用5期移动平均预测未来3期
        double[] forecast = ARIMAUtil.movingAverageWithTrend(data, 3, 5);
        
        System.out.println("移动平均预测: " + Arrays.toString(forecast));
    }
}
