package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.BranchRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRoleRepository extends JpaRepository<BranchRoleEntity, Long> {
    
    List<BranchRoleEntity> findByBranchIdOrderBySortOrderAsc(Integer branchId);
    
    List<BranchRoleEntity> findByBranchIdAndActiveOrderBySortOrderAsc(Integer branchId, boolean active);
    
    Optional<BranchRoleEntity> findByBranchIdAndCode(Integer branchId, String code);
    
    boolean existsByBranchIdAndCode(Integer branchId, String code);
}
