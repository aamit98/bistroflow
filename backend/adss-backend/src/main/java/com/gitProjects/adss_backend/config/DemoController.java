package com.gitProjects.adss_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for managing demo data.
 * Only active in "dev" profile for safety.
 * 
 * Endpoints:
 * - POST /api/admin/demo/seed - Seeds all demo data
 * - POST /api/admin/demo/reset - Clears and re-seeds all data
 * - GET /api/admin/demo/stats - Gets statistics about current data
 */
@RestController
@RequestMapping("/api/admin/demo")
@Profile({"dev", "test"})
public class DemoController {

    @Autowired
    private DemoDataSeeder seeder;

    /**
     * Seeds all demo data. Safe to call multiple times (idempotent for some entities).
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedData() {
        try {
            seeder.seedAll();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Demo data seeded successfully",
                "stats", seeder.getStats()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to seed data: " + e.getMessage()
            ));
        }
    }

    /**
     * Clears all data and re-seeds from scratch.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetData() {
        try {
            seeder.clearAllData();
            seeder.seedAll();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Demo data reset successfully",
                "stats", seeder.getStats()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to reset data: " + e.getMessage()
            ));
        }
    }

    /**
     * Gets statistics about the current data in the database.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "stats", seeder.getStats()
        ));
    }
}
