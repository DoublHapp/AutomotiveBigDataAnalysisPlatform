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
     * 根据父级地区ID查询子地区
     * @param parentRegionId 父级地区ID
     * @return 子地区列表
     */
    List<Region> findByParentRegionId(Long parentRegionId);
    
    /**
     * 查询所有顶级地区（没有父级地区的地区）
     * @return 顶级地区列表
     */
    List<Region> findByParentRegionIdIsNull();
    
    /**
     * 查询所有非顶级地区
     * @return 非顶级地区列表
     */
    List<Region> findByParentRegionIdIsNotNull();
    
    /**
     * 根据地区名称模糊查询地区
     * @param regionName 地区名称关键字
     * @return 地区列表
     */
    List<Region> findByRegionNameContaining(String regionName);
    
    /**
     * 查询所有地区及其父级地区信息（用于DTO转换）
     * @return 地区及父级信息列表
     */
    @Query("SELECT r FROM Region r LEFT JOIN FETCH r.parentRegion")
    List<Region> findAllWithParent();
}
