package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface EmployeeAvailabilityRepository
        extends JpaRepository<EmployeeAvailabilityEntity, Long> {

    List<EmployeeAvailabilityEntity> findByEmployeeIdAndWeekStart(
            Integer employeeId, LocalDate weekStart
    );

    List<EmployeeAvailabilityEntity> findByEmployeeIdInAndWeekStart(
            Collection<Integer> employeeIds,
            LocalDate weekStart
    );

    List<EmployeeAvailabilityEntity> findByEmployeeIdAndWeekStartBetween(
            Integer employeeId, LocalDate start, LocalDate end
    );

    List<EmployeeAvailabilityEntity> findByWeekStartAndDayOfWeekAndShiftType(
            LocalDate weekStart,
            ShiftEnums.DayOfWeekCode dayOfWeek,
            ShiftEnums.ShiftType shiftType
    );

    @Query("select count(distinct av.employeeId) from EmployeeAvailabilityEntity av " +
            "join EmployeeAccount acc on acc.employeeId = av.employeeId " +
            "where acc.branchId = :branchId and av.weekStart = :weekStart " +
            "and acc.hrManager = false and acc.superAdmin = false")
    long countEmployeesWithAvailability(@Param("branchId") Integer branchId,
                                        @Param("weekStart") LocalDate weekStart);
}
