package com.gitProjects.adss_backend.inventory.repo;

import com.gitProjects.adss_backend.inventory.model.BranchStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchStockRepository extends JpaRepository<BranchStockEntity, Long> {

    List<BranchStockEntity> findByBranchId(Integer branchId);

    @Query("select count(bs) from BranchStockEntity bs where bs.branchId = :branchId and bs.quantityOnHand < bs.reorderThreshold")
    long countLowStockItems(@Param("branchId") Integer branchId);
}
