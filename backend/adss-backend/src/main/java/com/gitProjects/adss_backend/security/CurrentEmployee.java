package com.gitProjects.adss_backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentEmployee {

    public static Integer getEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Integer)) {
            return null;
        }
        return (Integer) auth.getPrincipal();
    }

    public static Integer getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !(auth.getDetails() instanceof SecurityConfig.EmployeeAuthentication ea)) {
            return null;
        }
        return ea.getBranchId();
    }

    public static boolean isHrManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !(auth.getDetails() instanceof SecurityConfig.EmployeeAuthentication ea)) {
            return false;
        }
        return ea.isHrManager();
    }
}
