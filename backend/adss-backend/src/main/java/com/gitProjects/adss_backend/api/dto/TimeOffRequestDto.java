package com.gitProjects.adss_backend.api.dto;

import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.model.TimeOffRequestEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TimeOffRequestDto(
        Long id,
        Integer employeeId,
        Integer branchId,
        LocalDate date,
        ShiftEnums.ShiftType shiftType,
        String reason,
        TimeOffRequestEntity.Status status,
        Integer reviewedByEmployeeId,
        LocalDateTime reviewedAt,
        String decisionComment,
        LocalDateTime createdAt
) {}
