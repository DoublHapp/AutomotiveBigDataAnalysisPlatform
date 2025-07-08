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
}