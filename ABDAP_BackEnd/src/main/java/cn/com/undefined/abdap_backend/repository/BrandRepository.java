package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 品牌数据访问层
 */
@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    
}
