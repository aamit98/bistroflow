package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<RestaurantEntity, Long> {
    
    List<RestaurantEntity> findByActiveTrue();
    
    Optional<RestaurantEntity> findByHrManagerId(Integer hrManagerId);
    
    @Query("SELECT r FROM RestaurantEntity r LEFT JOIN FETCH r.branches WHERE r.id = :id")
    Optional<RestaurantEntity> findByIdWithBranches(@Param("id") Long id);
    
    boolean existsByName(String name);
}
