package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 地区数据访问层
 * 提供地区相关的数据库操作方法
 */
@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    /**
     * 根据地区名称查询地区信息
     */
    Region findByRegionName(String regionName);

    /**
     * 查询所有顶级地区
     * 
     * @return 顶级地区列表
     */
    @Query("SELECT DISTINCT r.parentRegion FROM Region r WHERE r.parentRegion IS NOT NULL")
    List<String> findParentRegions();

    /**
     * 根据父级地区查询子地区
     * @param parentRegion 父级地区名称
     * @return 子地区列表
     */
    List<Region> findByParentRegion(String parentRegion);

    /**
     * 根据父级地区列表查询子地区
     * @param parentRegions 父级地区名称列表
     * @return 子地区列表
     */
    @Query("SELECT r FROM Region r WHERE r.parentRegion IN :parentRegions")
    List<Region> findByParentRegions(List<String> parentRegions);
}
