package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.SaleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

/**
 * 销售记录数据访问层
 */
@Repository
public interface SaleRecordRepository extends JpaRepository<SaleRecord, Long> {

       /**
        * 根据车型ID查询销售记录
        */
       List<SaleRecord> findByCarModelId(Long carModelId);

       /**
        * 根据地区ID查询销售记录
        */
       List<SaleRecord> findByRegionId(Long regionId);

       /**
        * 根据车型ID和地区ID查询销售记录
        */
       List<SaleRecord> findByCarModelIdAndRegionId(Long carModelId, Long regionId);

       /**
        * 查询热门车型排行 - 按销量总和排序（指定时间范围）
        */
       @Query("SELECT sr.carModelId, SUM(sr.saleCount) as totalSales " +
                     "FROM SaleRecord sr " +
                     "WHERE sr.saleMonth >= :startDate " +
                     "GROUP BY sr.carModelId " +
                     "ORDER BY totalSales DESC")
       List<Object[]> findCarModelSalesRanking(@Param("startDate") LocalDate startDate);

       /**
        * 查询地区销量分布 - 按地区汇总销量（指定时间范围）
        */
       @Query("SELECT sr.regionId, SUM(sr.saleCount) as totalSales " +
                     "FROM SaleRecord sr " +
                     "WHERE sr.saleMonth >= :startDate " +
                     "GROUP BY sr.regionId " +
                     "ORDER BY totalSales DESC")
       List<Object[]> findRegionSalesDistribution(@Param("startDate") LocalDate startDate);

       /**
        * 查询指定车型的月度销量趋势
        */
       @Query("SELECT YEAR(sr.saleMonth), MONTH(sr.saleMonth), SUM(sr.saleCount) as monthlySales " +
                     "FROM SaleRecord sr " +
                     "WHERE sr.carModelId = :carModelId " +
                     "AND sr.saleMonth >= :startDate " +
                     "GROUP BY YEAR(sr.saleMonth), MONTH(sr.saleMonth) " +
                     "ORDER BY YEAR(sr.saleMonth), MONTH(sr.saleMonth)")
       List<Object[]> findMonthlySalesTrendByCarModelId(@Param("carModelId") Long carModelId,
                     @Param("startDate") LocalDate startDate);

       /**
        * 查询指定车型的月度销售额趋势
        */
       @Query("SELECT YEAR(sr.saleMonth), MONTH(sr.saleMonth), SUM(sr.saleCount * sr.saleAmount) as monthlyRevenue " +
                     "FROM SaleRecord sr " +
                     "WHERE sr.carModelId = :carModelId " +
                     "AND sr.saleMonth >= :startDate " +
                     "GROUP BY YEAR(sr.saleMonth), MONTH(sr.saleMonth) " +
                     "ORDER BY YEAR(sr.saleMonth), MONTH(sr.saleMonth)")
       List<Object[]> findMonthlyRevenueTrendByCarModelId(@Param("carModelId") Long carModelId,
                     @Param("startDate") LocalDate startDate);
}
