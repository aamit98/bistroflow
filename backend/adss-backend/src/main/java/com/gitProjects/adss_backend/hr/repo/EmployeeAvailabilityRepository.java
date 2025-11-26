package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.EmployeeAvailabilityEntity;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeAvailabilityRepository
        extends JpaRepository<EmployeeAvailabilityEntity, Long> {

    List<EmployeeAvailabilityEntity> findByEmployeeIdAndWeekStart(
            Integer employeeId, LocalDate weekStart
    );

    List<EmployeeAvailabilityEntity> findByEmployeeIdAndWeekStartBetween(
            Integer employeeId, LocalDate start, LocalDate end
    );

    List<EmployeeAvailabilityEntity> findByWeekStartAndDayOfWeekAndShiftType(
            LocalDate weekStart,
            ShiftEnums.DayOfWeekCode dayOfWeek,
            ShiftEnums.ShiftType shiftType
    );
}
