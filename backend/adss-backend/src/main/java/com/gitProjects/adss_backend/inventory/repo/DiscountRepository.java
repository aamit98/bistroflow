package com.gitProjects.adss_backend.inventory.repo;

import com.gitProjects.adss_backend.inventory.model.DiscountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountRepository extends JpaRepository<DiscountEntity, Long> {

    List<DiscountEntity> findByBranchId(Integer branchId);
}
