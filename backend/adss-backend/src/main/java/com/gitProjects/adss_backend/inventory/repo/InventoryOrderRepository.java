package com.gitProjects.adss_backend.inventory.repo;

import com.gitProjects.adss_backend.inventory.model.InventoryOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryOrderRepository extends JpaRepository<InventoryOrderEntity, Long> {

    List<InventoryOrderEntity> findByBranchId(Integer branchId);
}
