package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.ScheduleAssignment;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleAssignmentRepository extends JpaRepository<ScheduleAssignment, Long> {
    List<ScheduleAssignment> findByBranchIdAndShiftDate(Integer branchId, LocalDate shiftDate);
    List<ScheduleAssignment> findByBranchIdAndShiftDateAndShiftType(Integer branchId, LocalDate shiftDate, ShiftEnums.ShiftType shiftType);
    List<ScheduleAssignment> findByEmployeeIdAndShiftDateBetween(Integer employeeId, LocalDate startDate, LocalDate endDate);
    List<ScheduleAssignment> findByBranchIdAndShiftDateBetween(Integer branchId, LocalDate startDate, LocalDate endDate);

    Page<ScheduleAssignment> findByBranchIdAndShiftDateBetween(
        Integer branchId,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    );

    @Query("""
        select count(a.id)
          from ScheduleAssignment a
         where a.branchId = :branchId
           and a.shiftDate between :weekStart and :weekEnd
        """)
    long countAssignmentsInWeek(@Param("branchId") Integer branchId,
                @Param("weekStart") LocalDate weekStart,
                @Param("weekEnd") LocalDate weekEnd);

        @Query("""
                        select coalesce(a.role, 'UNASSIGNED') as role,
                                     count(a.id)                         as assignedCount
                            from ScheduleAssignment a
                         where a.branchId = :branchId
                             and a.shiftDate between :weekStart and :weekEnd
                         group by a.role
                        """)
    List<RoleAssignmentAggregate> aggregateAssignmentsByRole(
        @Param("branchId") Integer branchId,
        @Param("weekStart") LocalDate weekStart,
        @Param("weekEnd") LocalDate weekEnd
    );

    @Query("""
        select a.shiftDate                      as shiftDate,
           a.shiftType                      as shiftType,
           coalesce(a.role, 'UNASSIGNED')   as role,
           count(a.id)                      as assignedCount
          from ScheduleAssignment a
         where a.branchId = :branchId
           and a.shiftDate between :weekStart and :weekEnd
         group by a.shiftDate, a.shiftType, a.role
        """)
    List<ShiftCoverageAggregate> aggregateAssignmentsByShift(
        @Param("branchId") Integer branchId,
        @Param("weekStart") LocalDate weekStart,
        @Param("weekEnd") LocalDate weekEnd
    );

    interface RoleAssignmentAggregate {
    String getRole();
    long getAssignedCount();
    }

    interface ShiftCoverageAggregate {
    LocalDate getShiftDate();
    ShiftEnums.ShiftType getShiftType();
    String getRole();
    long getAssignedCount();
    }
}
