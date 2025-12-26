package com.gitProjects.adss_backend.service;

import com.gitProjects.adss_backend.api.dto.EmployeeSummaryDto;
import com.gitProjects.adss_backend.api.dto.PagedResponse;
import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class HrEmployeeManagementService {
    private static final int MAX_PAGE_SIZE = 200;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final EmployeeAccountRepository accountRepository;

    public HrEmployeeManagementService(EmployeeAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public PagedResponse<EmployeeSummaryDto> getEmployeesForBranch(int branchId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<EmployeeAccount> employees = accountRepository
                .findByBranchIdAndActiveTrueAndHrManagerFalseAndSuperAdminFalse(branchId, pageable);

        List<EmployeeSummaryDto> content = employees.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PagedResponse<>(
                content,
                employees.getNumber(),
                employees.getSize(),
                employees.getTotalElements(),
                employees.getTotalPages(),
                employees.isFirst(),
                employees.isLast()
        );
    }

    private EmployeeSummaryDto toSummary(EmployeeAccount account) {
        List<String> roles = Optional.ofNullable(account.getRoles())
                .map(List::copyOf)
                .orElse(List.of());

        int hourlyRate = Optional.ofNullable(account.getHourlyRate()).orElse(0);
        int monthlyRate = Optional.ofNullable(account.getMonthlyRate()).orElse(0);
        String terms = Optional.ofNullable(account.getTermsOfEmployment()).orElse("");
        String startDate = account.getStartDate() != null
                ? account.getStartDate().format(ISO_DATE)
                : "";
        String name = Optional.ofNullable(account.getName())
                .filter(n -> !n.isBlank())
                .orElse(account.getUsername());

        return new EmployeeSummaryDto(
                account.getEmployeeId(),
                account.getBranchId(),
                name,
                account.isHrManager(),
                account.isSuperAdmin(),
                hourlyRate,
                monthlyRate,
                terms,
                roles,
                startDate
        );
    }
}
