package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.Ranking;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {
       Ranking findByRankType(String rankingType);

       /**
        * 多表联合查询，返回车型基础信息、指定时间范围销量、上一年同期销量、口碑评分
        *
        * @param startDate 开始日期（yyyy-MM-dd）
        * @param endDate   结束日期（yyyy-MM-dd）
        * @param lastStart 上一年同期开始日期（yyyy-MM-dd）
        * @param lastEnd   上一年同期结束日期（yyyy-MM-dd）
        * @param region    地区（可为null，表示全部）
        * @return Object[]: [carModelId, modelName, modelFullName, brandId, brandName,
        *         level, launchDate, officialPrice, engineType, seatNum, driveType,
        *         rangeKm, imageUrl, 当前销量, 上年同期销量, 口碑评分]
        */
       @Query(value = "SELECT " +
                     "ANY_VALUE(c.car_model_id), c.model_name, ANY_VALUE(c.model_full_name), ANY_VALUE(b.brand_id), ANY_VALUE(b.brand_name), ANY_VALUE(c.level), ANY_VALUE(c.launch_date), "
                     +
                     "MAX(c.official_price), ANY_VALUE(c.engine_type), ANY_VALUE(c.seat_num), ANY_VALUE(c.drive_type), ANY_VALUE(c.range_km), ANY_VALUE(c.image_url), "
                     +
                     // 当前时间范围销量
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :startDate AND sr.sale_month <= :endDate THEN sr.sale_count ELSE 0 END), 0) AS currentSaleCount, "
                     +
                     // 上一年同期销量
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :lastStart AND sr.sale_month <= :lastEnd THEN sr.sale_count ELSE 0 END), 0) AS lastYearSaleCount, "
                     +
                     // 口碑评分
                     "COALESCE(AVG(op.score_current), 0) AS opinionScore " +
                     "FROM car_model c " +
                     "LEFT JOIN brand b ON c.brand_id = b.brand_id " +
                     "LEFT JOIN sale_record sr ON c.car_model_id = sr.car_model_id " +
                     "LEFT JOIN opinion op ON c.car_model_id = op.car_model_id " +
                     "LEFT JOIN region re ON re.region_id = sr.region_id " +
                     "WHERE (:region IS NULL OR re.region_name = :region OR re.parent_region = :region) " +
                     "GROUP BY c.model_name " +
                     // "GROUP BY c.model_name, b.brand_id, b.brand_name, c.level, c.launch_date, "
                     // +
                     // "c.official_price, c.engine_type, c.seat_num, c.drive_type, c.range_km,
                     // c.image_url " +
                     "ORDER BY currentSaleCount DESC", nativeQuery = true)
       List<Object[]> findCarModelSalesAndOpinion(
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("lastStart") String lastStart,
                     @Param("lastEnd") String lastEnd,
                     @Param("region") String region,
                     Pageable pageable);

       /**
        * 多表联合查询，返回地区基础信息、指定时间范围销量、上一年同期销量
        *
        * @param startDate    开始日期（yyyy-MM-dd）
        * @param endDate      结束日期（yyyy-MM-dd）
        * @param lastStart    上一年同期开始日期（yyyy-MM-dd）
        * @param lastEnd      上一年同期结束日期（yyyy-MM-dd）
        * @param parentRegion 上级地区
        * @return Object[]: [regionId, regionName, parentRegion, regionType, 当前销量,
        *         上年同期销量]
        */
       @Query(value = "SELECT " +
                     "re.region_id, re.region_name, re.parent_region, " +
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :startDate AND sr.sale_month <= :endDate THEN sr.sale_count ELSE 0 END), 0) AS currentSaleCount, "
                     +
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :lastStart AND sr.sale_month <= :lastEnd THEN sr.sale_count ELSE 0 END), 0) AS lastYearSaleCount "
                     +
                     "FROM region re " +
                     "LEFT JOIN sale_record sr ON re.region_id = sr.region_id " +
                     "WHERE (re.parent_region = :parentRegion) " +
                     "GROUP BY re.region_id, re.region_name, re.parent_region " +
                     "ORDER BY currentSaleCount DESC", nativeQuery = true)
       List<Object[]> findRegionSalesRanking(
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("lastStart") String lastStart,
                     @Param("lastEnd") String lastEnd,
                     @Param("parentRegion") String parentRegion,
                     Pageable pageable);

       @Query(value = "SELECT " +
                     "null as region_id, re.parent_region as region_name, null as parent_region, " +
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :startDate AND sr.sale_month <= :endDate THEN sr.sale_count ELSE 0 END), 0) AS currentSaleCount, "
                     +
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :lastStart AND sr.sale_month <= :lastEnd THEN sr.sale_count ELSE 0 END), 0) AS lastYearSaleCount "
                     +
                     "FROM region re " +
                     "LEFT JOIN sale_record sr ON re.region_id = sr.region_id " +
                     "GROUP BY re.parent_region " +
                     "ORDER BY currentSaleCount DESC", nativeQuery = true)
       List<Object[]> findRegionSalesRanking(
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("lastStart") String lastStart,
                     @Param("lastEnd") String lastEnd,
                     Pageable pageable);

       /**
        * 多表联合查询，返回车型基础信息、年油费预估、口碑总评分
        *
        * @param startDate 开始日期（yyyy-MM-dd）
        * @param endDate   结束日期（yyyy-MM-dd）
        * @param region    地区（可为null，表示全部）
        * @return Object[]: [carModelId, modelName, modelFullName, brandId, brandName,
        *         level, launchDate, officialPrice, engineType, seatNum, driveType,
        *         rangeKm, imageUrl, annualFuelCost, opinionScore]
        */
       @Query(value = "SELECT " +
                     "ANY_VALUE(c.car_model_id), c.model_name, ANY_VALUE(c.model_full_name), ANY_VALUE(b.brand_id), ANY_VALUE(b.brand_name), "
                     +
                     "ANY_VALUE(c.level), ANY_VALUE(c.launch_date), MAX(c.official_price), ANY_VALUE(c.engine_type), " +
                     "ANY_VALUE(c.seat_num), ANY_VALUE(c.drive_type), ANY_VALUE(c.range_km), ANY_VALUE(c.image_url), " +
                     // 年油费预估（单位L/100km，年行驶1.5万km，每升7.5元）
                     "COALESCE(ROUND(AVG(fu.avg_fuel) * 15000 / 100 * 7.5, 2), 0) AS annualFuelCost, " +
                     // 口碑总评分
                     "COALESCE(AVG(op.score_current), 0) AS opinionScore " +
                     "FROM car_model c " +
                     "LEFT JOIN brand b ON c.brand_id = b.brand_id " +
                     "LEFT JOIN opinion op ON c.car_model_id = op.car_model_id " +
                     "LEFT JOIN fuel_economy fu ON c.car_model_id = fu.car_model_id " +
                     // "LEFT JOIN sale_record sr ON c.car_model_id = sr.car_model_id " +
                     // "LEFT JOIN region re ON re.region_id = sr.region_id " +
                     // "WHERE (:region IS NULL OR re.region_name = :region OR re.parent_region =
                     // :region) " +
                     "WHERE (:level IS NULL OR c.level = :level) " +
                     "AND (:maxPrice IS NULL OR c.official_price <= :maxPrice) " +
                     "AND (:engineType IS NULL OR c.engine_type = :engineType) " +
                     "AND fu.avg_fuel IS NOT NULL " +
                     "GROUP BY c.model_name " +
                     "ORDER BY annualFuelCost ASC", nativeQuery = true)
       List<Object[]> findFuelConsumptionRanking(
                     // @Param("startDate") String startDate,
                     // @Param("endDate") String endDate,
                     // @Param("region") String region,
                     @Param("level") String level,
                     @Param("maxPrice") Double maxPrice,
                     @Param("engineType") String engineType,
                     Pageable pageable);

       /**
        * 多表联合查询，返回车型基础信息及购车推荐榜单各维度分数和总分
        *
        * @param minPrice   用户预算下限
        * @param maxPrice   用户预算上限
        * @param level      用户选择车型类别
        * @param engineType 用户选择能源类型
        * @param seatNum    用户需求座位数
        * @param factory    用户主机厂偏好
        * @param pageable   分页
        * @return Object[]: [carModelId, modelName, modelFullName, brandId, brandName,
        *         level, launchDate, officialPrice, engineType, seatNum, driveType,
        *         rangeKm, imageUrl,
        *         budgetScore, levelScore, engineTypeScore, seatNumScore, brandScore]
        */
       @Query(value = "SELECT " +
                     "c.car_model_id, c.model_name, c.model_full_name, b.brand_id, b.brand_name, "
                     +
                     "c.level, c.launch_date, c.official_price, c.engine_type, " +
                     "c.seat_num, c.drive_type, c.range_km, c.image_url, " +
                     // 匹配度打分各项
                     // 预算匹配分（30分，区间内满分，超出区间线性递减）
                     "CASE " +
                     "  WHEN :minPrice IS NOT NULL AND c.official_price < :minPrice THEN GREATEST(0, 30 - ((:minPrice - c.official_price) / (:minPrice)) * 30) "
                     +
                     "  WHEN :maxPrice IS NOT NULL AND c.official_price > :maxPrice THEN GREATEST(0, 30 - ((c.official_price - :maxPrice) / (:maxPrice)) * 30) "
                     +
                     "  ELSE 30 END AS budgetScore, " +
                     // 车型类别匹配分（25分）
                     "CASE WHEN :level IS NULL OR c.level = :level THEN 25 ELSE 0 END AS levelScore, " +
                     // 能源类型匹配分（20分）
                     "CASE WHEN :engineType IS NULL OR c.engine_type = :engineType THEN 20 ELSE 0 END AS engineTypeScore, "
                     +
                     // 座位数匹配分（15分，大于等于用户需求得满分，否则0）
                     "CASE WHEN :seatNum IS NULL OR c.seat_num >= :seatNum THEN 15 ELSE 0 END AS seatNumScore, " +
                     // 主机厂匹配分（10分）
                     "CASE WHEN :factory IS NULL OR b.factory = :factory THEN 10 ELSE 0 END AS brandScore " +
                     "FROM car_model c " +
                     "LEFT JOIN brand b ON c.brand_id = b.brand_id ", nativeQuery = true)
       List<Object[]> findCarModelMatchScoreRanking(
                     @Param("level") String level,
                     @Param("minPrice") Double minPrice,
                     @Param("maxPrice") Double maxPrice,
                     @Param("engineType") String engineType,
                     @Param("seatNum") Integer seatNum,
                     @Param("factory") String factory,
                     Pageable pageable);

       /**
        * 多表联合查询，返回车型基础信息、指定时间范围销量、上一年同期销量、口碑评分
        * 按口碑评分降序排序
        * 
        * @param startDate 开始日期（yyyy-MM-dd）
        * @param endDate   结束日期（yyyy-MM-dd）
        * @param lastStart 上一年同期开始日期（yyyy-MM-dd）
        * @param lastEnd   上一年同期结束日期（yyyy-MM-dd）
        * @param region    地区（可为null，表示全部）
        * @return Object[]: [carModelId, modelName, modelFullName, brandId, brandName,
        *         level, launchDate, officialPrice, engineType, seatNum, driveType,
        *         rangeKm, imageUrl, 当前销量, 上年同期销量, 口碑评分]
        */
       @Query(value = "SELECT " +
                     "ANY_VALUE(c.car_model_id), c.model_name, ANY_VALUE(c.model_full_name), ANY_VALUE(b.brand_id), ANY_VALUE(b.brand_name), ANY_VALUE(c.level), ANY_VALUE(c.launch_date), "
                     +
                     "MAX(c.official_price), ANY_VALUE(c.engine_type), ANY_VALUE(c.seat_num), ANY_VALUE(c.drive_type), ANY_VALUE(c.range_km), ANY_VALUE(c.image_url), "
                     +
                     // 当前时间范围销量
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :startDate AND sr.sale_month <= :endDate THEN sr.sale_count ELSE 0 END), 0) AS currentSaleCount, "
                     +
                     // 上一年同期销量
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :lastStart AND sr.sale_month <= :lastEnd THEN sr.sale_count ELSE 0 END), 0) AS lastYearSaleCount, "
                     +
                     // 口碑评分
                     "COALESCE(AVG(op.score_current), 0) AS opinionScore " +
                     "FROM car_model c " +
                     "LEFT JOIN brand b ON c.brand_id = b.brand_id " +
                     "LEFT JOIN sale_record sr ON c.car_model_id = sr.car_model_id " +
                     "LEFT JOIN opinion op ON c.car_model_id = op.car_model_id " +
                     "LEFT JOIN region re ON re.region_id = sr.region_id " +
                     "WHERE (:region IS NULL OR re.region_name = :region OR re.parent_region = :region) " +
                     "GROUP BY c.model_name " +
                     // "GROUP BY c.model_name, b.brand_id, b.brand_name, c.level, c.launch_date, "
                     // +
                     // "c.official_price, c.engine_type, c.seat_num, c.drive_type, c.range_km,
                     // c.image_url " +
                     "ORDER BY opinionScore DESC", nativeQuery = true)
       List<Object[]> findCarModelOpinionScoreRanking(
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("lastStart") String lastStart,
                     @Param("lastEnd") String lastEnd,
                     @Param("region") String region,
                     Pageable pageable);

       /**
        * 多表联合查询，返回车型基础信息、指定时间范围销量、上一年同期销量
        * 按市场份额降序排序（当前销量与市场份额成正比关系，因此实际按currentSaleCount排行）
        * 
        * @param startDate 开始日期（yyyy-MM-dd）
        * @param endDate   结束日期（yyyy-MM-dd）
        * @param lastStart 上一年同期开始日期（yyyy-MM-dd）
        * @param lastEnd   上一年同期结束日期（yyyy-MM-dd）
        * @param region    地区（可为null，表示全部）
        * @return Object[]: [carModelId, modelName, modelFullName, brandId, brandName,
        *         level, launchDate, officialPrice, engineType, seatNum, driveType,
        *         rangeKm, imageUrl, 当前销量, 上年同期销量]
        */
       @Query(value = "SELECT " +
                     "ANY_VALUE(c.car_model_id), c.model_name, ANY_VALUE(c.model_full_name), ANY_VALUE(b.brand_id), ANY_VALUE(b.brand_name), ANY_VALUE(c.level), ANY_VALUE(c.launch_date), "
                     +
                     "MAX(c.official_price), ANY_VALUE(c.engine_type), ANY_VALUE(c.seat_num), ANY_VALUE(c.drive_type), ANY_VALUE(c.range_km), ANY_VALUE(c.image_url), "
                     +
                     // 当前时间范围销量
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :startDate AND sr.sale_month <= :endDate THEN sr.sale_count ELSE 0 END), 0) AS currentSaleCount, "
                     +
                     // 上一年同期销量
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :lastStart AND sr.sale_month <= :lastEnd THEN sr.sale_count ELSE 0 END), 0) AS lastYearSaleCount "
                     +
                     "FROM car_model c " +
                     "LEFT JOIN brand b ON c.brand_id = b.brand_id " +
                     "LEFT JOIN sale_record sr ON c.car_model_id = sr.car_model_id " +
                     "LEFT JOIN region re ON re.region_id = sr.region_id " +
                     "WHERE (:region IS NULL OR re.region_name = :region OR re.parent_region = :region) " +
                     "GROUP BY c.model_name " +
                     "ORDER BY currentSaleCount DESC", nativeQuery = true)
       List<Object[]> findCarModelMarketShareRanking(
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("lastStart") String lastStart,
                     @Param("lastEnd") String lastEnd,
                     @Param("region") String region,
                     Pageable pageable);

       /**
        * 多表联合查询，返回车型基础信息、指定时间范围销量、上一年同期销量
        * 按销量增长率降序排序
        *
        * @param startDate 开始日期（yyyy-MM-dd）
        * @param endDate   结束日期（yyyy-MM-dd）
        * @param lastStart 上一年同期开始日期（yyyy-MM-dd）
        * @param lastEnd   上一年同期结束日期（yyyy-MM-dd）
        * @param region    地区（可为null，表示全部）
        * @return Object[]: [carModelId, modelName, modelFullName, brandId, brandName,
        *         level, launchDate, officialPrice, engineType, seatNum, driveType,
        *         rangeKm, imageUrl, 当前销量, 上年同期销量, 增长率]
        */
       @Query(value = "SELECT t.*, " +
                     "  (t.currentSaleCount * t.opinionScore / IFNULL(t.official_price, 0)) AS valueScore " +
                     "FROM (" +
                     "  SELECT " +
                     "    ANY_VALUE(c.car_model_id) AS car_model_id, c.model_name, ANY_VALUE(c.model_full_name) AS model_full_name, "
                     +
                     "    ANY_VALUE(b.brand_id) AS brand_id, ANY_VALUE(b.brand_name) AS brand_name, ANY_VALUE(c.level) AS level, "
                     +
                     "    ANY_VALUE(c.launch_date) AS launch_date, MAX(c.official_price) AS official_price, " +
                     "    ANY_VALUE(c.engine_type) AS engine_type, ANY_VALUE(c.seat_num) AS seat_num, " +
                     "    ANY_VALUE(c.drive_type) AS drive_type, ANY_VALUE(c.range_km) AS range_km, " +
                     "    ANY_VALUE(c.image_url) AS image_url, " +
                     "    COALESCE(SUM(CASE WHEN sr.sale_month >= :startDate AND sr.sale_month <= :endDate THEN sr.sale_count ELSE 0 END), 0) AS currentSaleCount, "
                     +
                     "    COALESCE(SUM(CASE WHEN sr.sale_month >= :lastStart AND sr.sale_month <= :lastEnd THEN sr.sale_count ELSE 0 END), 0) AS lastYearSaleCount, "
                     +
                     "    COALESCE(AVG(op.score_current), 0) AS opinionScore " +
                     "  FROM car_model c " +
                     "  LEFT JOIN brand b ON c.brand_id = b.brand_id " +
                     "  LEFT JOIN sale_record sr ON c.car_model_id = sr.car_model_id " +
                     "  LEFT JOIN opinion op ON c.car_model_id = op.car_model_id " +
                     "  LEFT JOIN region re ON re.region_id = sr.region_id " +
                     "  WHERE (:region IS NULL OR re.region_name = :region OR re.parent_region = :region) " +
                     "  GROUP BY c.model_name " +
                     ") t " +
                     "ORDER BY valueScore DESC", nativeQuery = true)
       List<Object[]> findCarModelGrowthRateRanking(
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("lastStart") String lastStart,
                     @Param("lastEnd") String lastEnd,
                     @Param("region") String region,
                     Pageable pageable);

       /**
        * 多表联合查询，返回车型基础信息、销量、销量增长率、市场份额、口碑总评分
        * 按性价比（价格/销量/口碑等）排序
        *
        * @param startDate 开始日期（yyyy-MM-dd）
        * @param endDate   结束日期（yyyy-MM-dd）
        * @param lastStart 上一年同期开始日期（yyyy-MM-dd）
        * @param lastEnd   上一年同期结束日期（yyyy-MM-dd）
        * @param region    地区（可为null，表示全部）
        * @return Object[]: [carModelId, modelName, modelFullName, brandId, brandName,
        *         level, launchDate, officialPrice, engineType, seatNum, driveType,
        *         rangeKm, imageUrl, 当前销量, 上年同期销量, 口碑总评分]
        */
       @Query(value = "SELECT " +
                     "ANY_VALUE(c.car_model_id), c.model_name, ANY_VALUE(c.model_full_name), ANY_VALUE(b.brand_id), ANY_VALUE(b.brand_name), "
                     +
                     "ANY_VALUE(c.level), ANY_VALUE(c.launch_date), MAX(c.official_price), ANY_VALUE(c.engine_type), " +
                     "ANY_VALUE(c.seat_num), ANY_VALUE(c.drive_type), ANY_VALUE(c.range_km), ANY_VALUE(c.image_url), " +
                     // 当前时间范围销量
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :startDate AND sr.sale_month <= :endDate THEN sr.sale_count ELSE 0 END), 0) AS currentSaleCount, "
                     +
                     // 上一年同期销量
                     "COALESCE(SUM(CASE WHEN sr.sale_month >= :lastStart AND sr.sale_month <= :lastEnd THEN sr.sale_count ELSE 0 END), 0) AS lastYearSaleCount, "
                     +
                     // 口碑总评分
                     "COALESCE(AVG(op.score_current), 0) AS opinionScore, " +
                     // 性价比分数（销量*口碑/价格，价格加1防止除0）
                     "(COALESCE(SUM(CASE WHEN sr.sale_month >= :startDate AND sr.sale_month <= :endDate THEN sr.sale_count ELSE 0 END), 0) "
                     +
                     " * COALESCE(AVG(op.score_current), 0) / (MAX(c.official_price) + 1)) AS valueScore " +
                     "FROM car_model c " +
                     "LEFT JOIN brand b ON c.brand_id = b.brand_id " +
                     "LEFT JOIN sale_record sr ON c.car_model_id = sr.car_model_id " +
                     "LEFT JOIN opinion op ON c.car_model_id = op.car_model_id " +
                     "LEFT JOIN region re ON re.region_id = sr.region_id " +
                     "WHERE (:region IS NULL OR re.region_name = :region OR re.parent_region = :region) " +
                     "GROUP BY c.model_name " +
                     "ORDER BY valueScore DESC", nativeQuery = true)
       List<Object[]> findCarModelValueRanking(
                     @Param("startDate") String startDate,
                     @Param("endDate") String endDate,
                     @Param("lastStart") String lastStart,
                     @Param("lastEnd") String lastEnd,
                     @Param("region") String region,
                     Pageable pageable);
}