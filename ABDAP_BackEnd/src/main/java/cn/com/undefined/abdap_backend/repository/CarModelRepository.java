package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.dto.CarModelDTO;
import cn.com.undefined.abdap_backend.dto.CarModelDetailDTO;
import cn.com.undefined.abdap_backend.entity.CarModel;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 车型数据访问层
 */
@Repository
public interface CarModelRepository extends JpaRepository<CarModel, Long> {
        @Query("SELECT DISTINCT c.level FROM CarModel c")
        List<String> findAllLevels();

        @Query("SELECT DISTINCT c.engineType FROM CarModel c")
        List<String> findAllEngineTypes();

        @Query("SELECT DISTINCT b.factory FROM Brand b WHERE (:keyword IS NULL OR b.factory LIKE %:keyword%)")
        List<String> findFactorysByKeyword(@Param("keyword") String keyword);

        @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.CarModelDTO(" +
                        "c.carModelId, c.modelName, c.modelFullName, c.brandId, " +
                        "b.brandName, c.level, c.launchDate, c.officialPrice, " +
                        "c.engineType, c.seatNum, c.driveType, c.rangeKm, c.imageUrl) " +
                        "FROM CarModel c LEFT JOIN c.brand b " +
                        "ORDER BY c.carModelId")
        Page<CarModelDTO> findAllCarModelDTOsByPage(Pageable pageable);

        @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.CarModelDTO(" +
                        "c.carModelId, c.modelName, c.modelFullName, c.brandId, " +
                        "b.brandName, c.level, c.launchDate, c.officialPrice, " +
                        "c.engineType, c.seatNum, c.driveType, c.rangeKm, c.imageUrl) " +
                        "FROM CarModel c LEFT JOIN c.brand b ")
        List<CarModelDTO> findAllCarModelDTOs();

        @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.CarModelDTO(" +
                        "c.carModelId, c.modelName, c.modelFullName, c.brandId, " +
                        "b.brandName, c.level, c.launchDate, c.officialPrice, " +
                        "c.engineType, c.seatNum, c.driveType, c.rangeKm, c.imageUrl) " +
                        "FROM CarModel c LEFT JOIN c.brand b " +
                        "WHERE c.carModelId = :carModelId")
        CarModelDTO findCarModelDTOById(@Param("carModelId") Long carModelId);

        @Query("SELECT new cn.com.undefined.abdap_backend.dto.CarModelDTO(" +
                        "c.carModelId, c.modelName, c.modelFullName, c.brandId, " +
                        "b.brandName, c.level, c.launchDate, c.officialPrice, " +
                        "c.engineType, c.seatNum, c.driveType, c.rangeKm, c.imageUrl) " +
                        "FROM CarModel c LEFT JOIN c.brand b " +
                        "WHERE (:keyword IS NULL OR c.modelName LIKE %:keyword%) " +
                        "ORDER BY c.carModelId")
        List<CarModelDTO> findTopByKeyword(@Param("keyword") String keyword, Pageable pageable);

        @Query("SELECT new cn.com.undefined.abdap_backend.dto.CarModelDetailDTO(" +
                        "c.carModelId, c.modelName, c.modelFullName, c.brandId, b.brandName, c.level, c.launchDate, c.officialPrice, "
                        +
                        "c.engineType, c.seatNum, c.driveType, c.rangeKm, c.imageUrl, " +
                        "AVG(op.powerScore), AVG(op.controlScore), AVG(op.comfortScore), AVG(op.appearanceScore), " +
                        "AVG(op.configScore), AVG(op.totalScore), " +
                        "SUM(COALESCE(sr.saleCount, 0))" +
                        ") " +
                        "FROM CarModel c " +
                        "LEFT JOIN c.brand b " +
                        "LEFT JOIN Opinion op ON op.carModelId = c.carModelId " +
                        "LEFT JOIN SaleRecord sr ON sr.carModelId = c.carModelId " +
                        "WHERE c.carModelId = :carModelId " +
                        "GROUP BY c.carModelId, c.modelName, c.modelFullName, c.brandId, b.brandName, c.level, c.launchDate, c.officialPrice, "
                        +
                        "c.engineType, c.seatNum, c.driveType, c.rangeKm, c.imageUrl")
        CarModelDetailDTO findCarModelDetailDTOById(@Param("carModelId") Long carModelId);
}
