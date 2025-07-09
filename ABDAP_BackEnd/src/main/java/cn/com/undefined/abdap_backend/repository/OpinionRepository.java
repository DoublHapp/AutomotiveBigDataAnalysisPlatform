package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.dto.OpinionDTO;
import cn.com.undefined.abdap_backend.entity.Opinion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 口碑评价数据访问层
 */
@Repository
public interface OpinionRepository extends JpaRepository<Opinion, Long> {
    @Query("SELECT new cn.com.undefined.abdap_backend.dto.OpinionDTO(" +
            "o.opinionId, o.carModelId, c.modelName, b.brandName, o.totalScore) " +
            "FROM Opinion o " +
            "LEFT JOIN CarModel c ON o.carModelId = c.carModelId " +
            "LEFT JOIN Brand b ON c.brandId = b.brandId " +
            "ORDER BY o.opinionId")
    List<OpinionDTO> findAllOpinionDTOs();
}
