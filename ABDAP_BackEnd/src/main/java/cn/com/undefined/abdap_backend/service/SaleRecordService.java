package cn.com.undefined.abdap_backend.service;

import cn.com.undefined.abdap_backend.dto.CarModelRankingDTO;
import cn.com.undefined.abdap_backend.dto.MonthlySalesTrendDTO;
import cn.com.undefined.abdap_backend.dto.MonthlyRevenueTrendDTO;
import cn.com.undefined.abdap_backend.dto.RegionSalesDTO;
import cn.com.undefined.abdap_backend.entity.CarModel;
import cn.com.undefined.abdap_backend.entity.Region;
import cn.com.undefined.abdap_backend.entity.SaleRecord;
import cn.com.undefined.abdap_backend.repository.CarModelRepository;
import cn.com.undefined.abdap_backend.repository.RegionRepository;
import cn.com.undefined.abdap_backend.repository.SaleRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 销售记录业务逻辑层
 */
@Service
@Transactional(readOnly = true)
public class SaleRecordService {

    @Autowired
    private SaleRecordRepository repository;
    
    @Autowired
    private CarModelRepository carModelRepository;
    
    @Autowired
    private RegionRepository regionRepository;

    /**
     * 查询所有销售记录
     */
    public List<SaleRecord> findAll() {
        return repository.findAll();
    }

    /**
     * 根据车型ID查询销售记录
     */
    public List<SaleRecord> findByCarModelId(Long carModelId) {
        return repository.findByCarModelId(carModelId);
    }

    /**
     * 根据地区ID查询销售记录
     */
    public List<SaleRecord> findByRegionId(Long regionId) {
        return repository.findByRegionId(regionId);
    }

    /**
     * 根据地区名称查询销售记录
     */
    public List<SaleRecord> findByRegionName(String regionName) {
        // 先根据地区名称查找地区ID
        Region region = regionRepository.findByRegionName(regionName);
        if (region == null) {
            // 如果地区不存在，返回空列表
            return new ArrayList<>();
        }
        
        // 根据地区ID查询销售记录
        return repository.findByRegionId(region.getRegionId());
    }

    /**
     * 根据车型ID和地区名称查询销售记录
     */
    public List<SaleRecord> findByCarModelIdAndRegionName(Long carModelId, String regionName) {
        // 先根据地区名称查找地区ID
        Region region = regionRepository.findByRegionName(regionName);
        if (region == null) {
            // 如果地区不存在，返回空列表
            return new ArrayList<>();
        }
        
        // 根据车型ID和地区ID查询销售记录
        return repository.findByCarModelIdAndRegionId(carModelId, region.getRegionId());
    }

    /**
     * 获取热门车型排行榜
     */
    public List<CarModelRankingDTO> getCarModelRanking(int limit) {
        // 默认查询一年内的数据
        LocalDate startDate = LocalDate.now().minusYears(1);
        return getCarModelRanking(limit, startDate);
    }
    
    /**
     * 获取热门车型排行榜（指定时间范围）
     */
    public List<CarModelRankingDTO> getCarModelRanking(int limit, LocalDate startDate) {
        List<Object[]> rawData = repository.findCarModelSalesRanking(startDate);
        List<CarModelRankingDTO> rankings = new ArrayList<>();
        
        int rank = 1;
        for (Object[] row : rawData) {
            if (rank > limit) break;  // 在Service层控制limit
            
            Long carModelId = (Long) row[0];
            Long totalSales = (Long) row[1];
            
            // 获取车型名称
            String carModelName = "未知车型";
            Optional<CarModel> carModel = carModelRepository.findById(carModelId);
            if (carModel.isPresent()) {
                carModelName = carModel.get().getModelName();
            }
            
            rankings.add(new CarModelRankingDTO(carModelId, carModelName, totalSales, rank));
            rank++;
        }
        
        return rankings;
    }
    
    /**
     * 获取地区销量分布
     */
    public List<RegionSalesDTO> getRegionSalesDistribution() {
        // 默认查询一年内的数据
        LocalDate startDate = LocalDate.now().minusYears(1);
        return getRegionSalesDistribution(startDate);
    }
    
    /**
     * 获取地区销量分布（指定时间范围）
     */
    public List<RegionSalesDTO> getRegionSalesDistribution(LocalDate startDate) {
        List<Object[]> rawData = repository.findRegionSalesDistribution(startDate);
        List<RegionSalesDTO> distributions = new ArrayList<>();
        
        for (Object[] row : rawData) {
            Long regionId = (Long) row[0];
            Long totalSales = (Long) row[1];
            
            // 获取地区名称
            String regionName = "未知地区";
            Optional<Region> region = regionRepository.findById(regionId);
            if (region.isPresent()) {
                regionName = region.get().getRegionName();
            }
            
            distributions.add(new RegionSalesDTO(regionId, regionName, totalSales));
        }
        
        return distributions;
    }
    
    /**
     * 根据时间跨度类型计算开始日期
     */
    public LocalDate calculateStartDate(String timeSpan) {
        LocalDate now = LocalDate.now();
        switch (timeSpan.toLowerCase()) {
            case "month":
            case "一月":
                return now.minusMonths(1);
            case "half_year":
            case "半年":
                return now.minusMonths(6);
            case "year":
            case "一年":
            default:
                return now.minusYears(1);
        }
    }
    
    /**
     * 获取指定车型的月度销量趋势
     */
    public List<MonthlySalesTrendDTO> getMonthlySalesTrend(Long carModelId) {
        // 默认查询一年内的数据
        LocalDate startDate = LocalDate.now().minusYears(1);
        return getMonthlySalesTrend(carModelId, startDate);
    }
    
    /**
     * 获取指定车型的月度销量趋势（指定时间范围）
     */
    public List<MonthlySalesTrendDTO> getMonthlySalesTrend(Long carModelId, LocalDate startDate) {
        List<Object[]> rawData = repository.findMonthlySalesTrendByCarModelId(carModelId, startDate);
        List<MonthlySalesTrendDTO> trends = new ArrayList<>();
        
        for (Object[] row : rawData) {
            Integer year = (Integer) row[0];
            Integer month = (Integer) row[1];
            Long salesCount = (Long) row[2];
            
            trends.add(new MonthlySalesTrendDTO(year, month, salesCount));
        }
        
        return trends;
    }
    
    /**
     * 获取指定车型的月度销售额趋势
     */
    public List<MonthlyRevenueTrendDTO> getMonthlyRevenueTrend(Long carModelId) {
        // 默认查询一年内的数据
        LocalDate startDate = LocalDate.now().minusYears(1);
        return getMonthlyRevenueTrend(carModelId, startDate);
    }
    
    /**
     * 获取指定车型的月度销售额趋势（指定时间范围）
     */
    public List<MonthlyRevenueTrendDTO> getMonthlyRevenueTrend(Long carModelId, LocalDate startDate) {
        List<Object[]> rawData = repository.findMonthlyRevenueTrendByCarModelId(carModelId, startDate);
        List<MonthlyRevenueTrendDTO> trends = new ArrayList<>();
        
        for (Object[] row : rawData) {
            Integer year = (Integer) row[0];
            Integer month = (Integer) row[1];
            BigDecimal revenue = (BigDecimal) row[2];
            
            trends.add(new MonthlyRevenueTrendDTO(year, month, revenue));
        }
        
        return trends;
    }

}
