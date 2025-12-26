package com.gitProjects.adss_backend.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeAccountRepository
        extends JpaRepository<EmployeeAccount, Long> {

    Optional<EmployeeAccount> findByEmployeeId(Integer employeeId);

    Optional<EmployeeAccount> findByUsername(String username);

    List<EmployeeAccount> findByBranchId(Integer branchId);

    Page<EmployeeAccount> findByBranchIdAndActiveTrueAndHrManagerFalseAndSuperAdminFalse(
            Integer branchId,
            Pageable pageable
    );

    long countByBranchId(Integer branchId);

    long countByBranchIdAndHrManagerTrue(Integer branchId);

                @Query("select e from EmployeeAccount e where e.branchId = :branchId and e.hrManager = true")
                List<EmployeeAccount> findHrManagersByBranchId(Integer branchId);

                @Query("select e from EmployeeAccount e where e.restaurantId = :restaurantId and e.hrManager = true")
                List<EmployeeAccount> findHrManagersByRestaurantId(Long restaurantId);

                @Query("""
                                                select e.employeeId as employeeId,
                                                                         e.branchId    as branchId,
                                                                         e.restaurantId as restaurantId,
                                                                         coalesce(e.name, e.username) as name
                                                        from EmployeeAccount e
                                                 where e.branchId = :branchId
                                                         and e.hrManager = true
                                                         and e.active = true
                                                """)
                List<HrManagerContact> findHrManagerContactsByBranchId(Integer branchId);

                @Query("""
                                                select e.employeeId as employeeId,
                                                                         e.branchId    as branchId,
                                                                         e.restaurantId as restaurantId,
                                                                         coalesce(e.name, e.username) as name
                                                        from EmployeeAccount e
                                                 where e.restaurantId = :restaurantId
                                                         and e.hrManager = true
                                                         and e.active = true
                                                """)
                List<HrManagerContact> findHrManagerContactsByRestaurantId(Long restaurantId);

    List<EmployeeAccount> findByEmployeeIdIn(Iterable<Integer> employeeIds);

                interface HrManagerContact {
                                Integer getEmployeeId();
                                Integer getBranchId();
                                Long getRestaurantId();
                                String getName();
                }
}