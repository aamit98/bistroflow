package com.gitProjects.adss_backend.inventory.repo;

import com.gitProjects.adss_backend.inventory.model.InventoryOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryOrderRepository extends JpaRepository<InventoryOrderEntity, Long> {

    List<InventoryOrderEntity> findByBranchId(Integer branchId);

    @Query("select count(o) from InventoryOrderEntity o where o.branchId = :branchId and o.status in :statuses")
    long countByBranchIdAndStatusIn(@Param("branchId") Integer branchId,
                                    @Param("statuses") List<String> statuses);
}
