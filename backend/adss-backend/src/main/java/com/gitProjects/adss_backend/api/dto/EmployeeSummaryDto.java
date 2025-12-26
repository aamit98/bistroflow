package com.gitProjects.adss_backend.api.dto;

import java.util.List;

public record EmployeeSummaryDto(
        int id,
        Integer branchId,
        String name,
        boolean isHRManager,
        boolean isSuperAdmin,
        int hourlyRate,
        int monthlyRate,
        String termsOfEmployment,
        List<String> roles,
        String startDate
) {}
