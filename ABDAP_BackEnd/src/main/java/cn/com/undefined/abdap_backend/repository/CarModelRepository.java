package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.dto.CarModelDTO;
import cn.com.undefined.abdap_backend.entity.CarModel;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 车型数据访问层
 */
@Repository
public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.CarModelDTO(" +
            "c.carModelId, c.modelName, c.modelFullName, c.brandId, " +
            "b.brandName, c.level, c.launchDate, c.officialPrice, " +
            "c.engineType, c.seatNum, c.driveType, c.rangeKm, c.imageUrl) " +
            "FROM CarModel c LEFT JOIN c.brand b " +
            "ORDER BY c.carModelId")
    List<CarModelDTO> findAllCarModelDTOsByPage(Pageable pageable);

    @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.CarModelDTO(" +
            "c.carModelId, c.modelName, c.modelFullName, c.brandId, " +
            "b.brandName, c.level, c.launchDate, c.officialPrice, " +
            "c.engineType, c.seatNum, c.driveType, c.rangeKm, c.imageUrl) " +
            "FROM CarModel c LEFT JOIN c.brand b ")
    List<CarModelDTO> findAllCarModelDTOsByPage();
}
