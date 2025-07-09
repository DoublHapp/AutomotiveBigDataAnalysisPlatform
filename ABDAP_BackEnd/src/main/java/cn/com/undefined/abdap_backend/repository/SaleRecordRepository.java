package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.dto.SaleRecordDTO;
import cn.com.undefined.abdap_backend.entity.SaleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
        * 指定多个车型id、只返回符合条件的销售记录
        */
       @Query("SELECT sr FROM SaleRecord sr WHERE sr.carModelId IN :carModelIds")
       List<SaleRecord> findByCarModelIds(@Param("carModelIds") List<Long> carModelIds);

       /**
        * 指定多个地区id、只返回符合条件的销售记录
        */
       @Query("SELECT sr FROM SaleRecord sr WHERE sr.regionId IN :regionIds")
       List<SaleRecord> findByRegionIds(@Param("regionIds") List<Long> regionIds);

       /**
        * 指定多个车型id、多个地区id，只返回符合条件的销售记录
        */
       @Query("SELECT sr FROM SaleRecord sr WHERE sr.carModelId IN :carModelIds AND sr.regionId IN :regionIds")
       List<SaleRecord> findByCarModelIdsAndRegionIds(@Param("carModelIds") List<Long> carModelIds,
                     @Param("regionIds") List<Long> regionIds);

       /**
        * 查询所有销售记录DTO
        */
       @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.SaleRecordDTO(" +
                     "sr.saleId, sr.carModelId, c.modelName, sr.regionId, " +
                     "r.regionName, sr.saleMonth, sr.saleCount, sr.saleAmount) " +
                     "FROM SaleRecord sr " +
                     "LEFT JOIN sr.carModel c " +
                     "LEFT JOIN sr.region r " +
                     "ORDER BY sr.saleId")
       List<SaleRecordDTO> findAllSaleRecordDTOs();

       /**
        * 根据车型ID列表查询销售记录DTO
        */
       @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.SaleRecordDTO(" +
                     "sr.saleId, sr.carModelId, c.modelName, sr.regionId, " +
                     "r.regionName, sr.saleMonth, sr.saleCount, sr.saleAmount) " +
                     "FROM SaleRecord sr " +
                     "LEFT JOIN sr.carModel c " +
                     "LEFT JOIN sr.region r " +
                     "WHERE sr.carModelId IN :carModelIds " +
                     "ORDER BY sr.saleId")
       List<SaleRecordDTO> findSaleRecordDTOsByCarModelIds(@Param("carModelIds") List<Long> carModelIds);

       /**
        * 根据地区ID列表查询销售记录DTO
        */
       @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.SaleRecordDTO(" +
                     "sr.saleId, sr.carModelId, c.modelName, sr.regionId, " +
                     "r.regionName, sr.saleMonth, sr.saleCount, sr.saleAmount) " +
                     "FROM SaleRecord sr " +
                     "LEFT JOIN sr.carModel c " +
                     "LEFT JOIN sr.region r " +
                     "WHERE sr.regionId IN :regionIds " +
                     "ORDER BY sr.saleId")
       List<SaleRecordDTO> findSaleRecordDTOsByRegionIds(@Param("regionIds") List<Long> regionIds);

       /**
        * 根据车型ID列表和地区ID列表查询销售记录DTO
        */
       @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.SaleRecordDTO(" +
                     "sr.saleId, sr.carModelId, c.modelName, sr.regionId, " +
                     "r.regionName, sr.saleMonth, sr.saleCount, sr.saleAmount) " +
                     "FROM SaleRecord sr " +
                     "LEFT JOIN sr.carModel c " +
                     "LEFT JOIN sr.region r " +
                     "WHERE sr.carModelId IN :carModelIds AND sr.regionId IN :regionIds " +
                     "ORDER BY sr.saleId")
       List<SaleRecordDTO> findSaleRecordDTOsByCarModelIdsAndRegionIds(
                     @Param("carModelIds") List<Long> carModelIds,
                     @Param("regionIds") List<Long> regionIds);

       /**
        * 按月份、地区、车型统计销售汇总数据
        *
        * @param startMonth 开始月份（格式：yyyy-MM-dd，建议为月初日期）
        * @param endMonth   结束月份（格式：yyyy-MM-dd，建议为月末日期）
        * @param region     地区名称（为null时统计全部地区）
        * @param carModel   车型名称（为null时统计全部车型）
        * @return 每行包含：month（String）、region（String）、carModel（String）、saleCount（Long）、saleAmount（BigDecimal）
        */
       @Query(value = "SELECT " +
                     "DATE_FORMAT(sr.sale_month, '%Y-%m') as month, " +
                     "COALESCE(:region, 'all') as region, " +
                     "COALESCE(:carModel, 'all') as carModel, " +
                     "SUM(sr.sale_count) as saleCount, " +
                     "SUM(sr.sale_amount) as saleAmount " +
                     "FROM sale_record sr " +
                     "LEFT JOIN region r ON sr.region_id = r.region_id " +
                     "LEFT JOIN car_model c ON sr.car_model_id = c.car_model_id " +
                     "WHERE sr.sale_month >= :startMonth AND sr.sale_month <= :endMonth " +
                     "AND (:region IS NULL OR r.region_name = :region OR r.parent_region - :region) " +
                     "AND (:carModel IS NULL OR c.model_name = :carModel) " +
                     "GROUP BY month, region, carModel " +
                     "ORDER BY month, region, carModel", nativeQuery = true)
       List<Object[]> findMonthlySummary(
                     @Param("startMonth") String startMonth,
                     @Param("endMonth") String endMonth,
                     @Param("region") String region,
                     @Param("carModel") String carModel);

       /**
        * 查询指定时间范围和地区的总销量（原生SQL实现）
        */
       @Query(value = "SELECT SUM(sr.sale_count) FROM sale_record sr " +
                     "LEFT JOIN region r ON sr.region_id = r.region_id " +
                     "WHERE sr.sale_month >= :startMonth AND sr.sale_month <= :endMonth " +
                     "AND (:region IS NULL OR r.region_name = :region OR r.parent_region = :region)", nativeQuery = true)
       BigDecimal findTotalSalesByMonthAndRegion(
                     @Param("startMonth") String startMonth,
                     @Param("endMonth") String endMonth,
                     @Param("region") String region);

       /**
        * 查询指定时间范围和地区的市场份额统计（原生SQL实现）
        * 
        * @param startMonth 开始月份（格式：yyyy-MM-dd，建议为月初日期）
        * @param endMonth   结束月份（格式：yyyy-MM-dd，建议为
        * @param region     地区名称（为null时统计全部地区）
        * @param carModel   车型名称（为null时统计全部车型）
        * @return 每行包含：marketShare（BigDecimal）、totalSaleCount（Long）、totalSaleAmount（BigDecimal）
        */
       @Query(value = "SELECT " +
                     "IFNULL(SUM(CASE WHEN c.model_name = :carModel THEN sr.sale_count ELSE 0 END) / NULLIF(SUM(sr.sale_count), 0), 0) AS marketShare, "
                     +
                     "SUM(sr.sale_count) AS totalSaleCount, " +
                     "SUM(sr.sale_amount) AS totalSaleAmount " +
                     "FROM sale_record sr " +
                     "LEFT JOIN region r ON sr.region_id = r.region_id " +
                     "LEFT JOIN car_model c ON sr.car_model_id = c.car_model_id " +
                     "WHERE sr.sale_month >= :startMonth AND sr.sale_month <= :endMonth " +
                     "AND (:region IS NULL OR r.region_name = :region OR r.parent_region = :region)", nativeQuery = true)
       List<Object[]> findMarketShareSummary(
                     @Param("startMonth") String startMonth,
                     @Param("endMonth") String endMonth,
                     @Param("region") String region,
                     @Param("carModel") String carModel);
}
