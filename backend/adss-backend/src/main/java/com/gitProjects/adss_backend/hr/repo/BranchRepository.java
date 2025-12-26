package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.BranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BranchRepository extends JpaRepository<BranchEntity, Integer> {
    
    List<BranchEntity> findByActiveTrue();
    
    List<BranchEntity> findByCity(String city);
    
    // Find all branches under a specific restaurant
    @Query("SELECT b FROM BranchEntity b WHERE b.restaurant.id = :restaurantId AND b.active = true")
    List<BranchEntity> findByRestaurantIdAndActiveTrue(@Param("restaurantId") Long restaurantId);
    
    @Query("SELECT b FROM BranchEntity b WHERE b.restaurant.id = :restaurantId")
    List<BranchEntity> findByRestaurantId(@Param("restaurantId") Long restaurantId);
    
    // Count branches for a restaurant
    @Query("SELECT COUNT(b) FROM BranchEntity b WHERE b.restaurant.id = :restaurantId AND b.active = true")
    long countByRestaurantId(@Param("restaurantId") Long restaurantId);
}
