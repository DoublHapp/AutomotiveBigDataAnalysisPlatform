package cn.com.undefined.abdap_backend.repository;

import cn.com.undefined.abdap_backend.entity.SaleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 销售数据Repository
 */
@Repository
public interface SalesRepository extends JpaRepository<SaleRecord, Long> {
    
}
