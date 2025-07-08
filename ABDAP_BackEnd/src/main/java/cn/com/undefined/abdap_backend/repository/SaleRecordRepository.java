package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.dto.SaleRecordDTO;
import cn.com.undefined.abdap_backend.entity.SaleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
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
                     "AND (:region IS NULL OR r.region_name = :region) " +
                     "AND (:carModel IS NULL OR c.model_name = :carModel) " +
                     "GROUP BY month, region, carModel " +
                     "ORDER BY month, region, carModel", nativeQuery = true)
       List<Object[]> findMonthlySummary(
                     @Param("startMonth") String startMonth,
                     @Param("endMonth") String endMonth,
                     @Param("region") String region,
                     @Param("carModel") String carModel);
}
