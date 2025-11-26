package com.gitProjects.adss_backend.inventory.repo;

import com.gitProjects.adss_backend.inventory.model.BranchStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchStockRepository extends JpaRepository<BranchStockEntity, Long> {

    List<BranchStockEntity> findByBranchId(Integer branchId);
}
