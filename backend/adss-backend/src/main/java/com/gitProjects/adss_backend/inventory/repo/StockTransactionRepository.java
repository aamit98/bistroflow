package com.gitProjects.adss_backend.inventory.repo;

import com.gitProjects.adss_backend.inventory.model.StockTransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockTransactionRepository extends JpaRepository<StockTransactionEntity, Long> {
    
    List<StockTransactionEntity> findByBranchIdOrderByTransactionDateDesc(Integer branchId);
    
    Page<StockTransactionEntity> findByBranchId(Integer branchId, Pageable pageable);
    
    List<StockTransactionEntity> findByBranchIdAndProductId(Integer branchId, Long productId);
    
    @Query("SELECT st FROM StockTransactionEntity st WHERE st.branchId = :branchId " +
           "AND st.transactionDate >= :startDate AND st.transactionDate <= :endDate " +
           "ORDER BY st.transactionDate DESC")
    List<StockTransactionEntity> findByBranchIdAndDateRange(
            @Param("branchId") Integer branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT st.type, SUM(st.quantity) FROM StockTransactionEntity st " +
           "WHERE st.branchId = :branchId AND st.product.id = :productId " +
           "GROUP BY st.type")
    List<Object[]> getTransactionSummaryByProduct(
            @Param("branchId") Integer branchId,
            @Param("productId") Long productId);
}
