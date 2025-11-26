package com.gitProjects.adss_backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeAccountRepository
        extends JpaRepository<EmployeeAccount, Long> {

    Optional<EmployeeAccount> findByEmployeeId(Integer employeeId);

    Optional<EmployeeAccount> findByUsername(String username);

    List<EmployeeAccount> findByBranchId(Integer branchId);
}