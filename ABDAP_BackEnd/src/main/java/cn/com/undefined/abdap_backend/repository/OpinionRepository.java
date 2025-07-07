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
    @Query(value = "SELECT new cn.com.undefined.abdap_backend.dto.OpinionDTO(" +
            "o.opinionId, o.carModelId, c.modelName, b.brandName, o.score) " +
            "FROM Opinion o " +
            "LEFT JOIN o.carModel c " +
            "LEFT JOIN c.brand b " +
            "ORDER BY o.opinionId")
    List<OpinionDTO> findAllOpinionDTOs();
}
