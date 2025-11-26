package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByEmployeeIdOrderByCreatedAtDesc(Integer employeeId);

    long countByEmployeeIdAndReadFalse(Integer employeeId);
    void deleteByEmployeeId(Integer employeeId);
}
