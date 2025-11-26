package com.gitProjects.adss_backend.config;

import DomainLayer.Inventory.CallBack;
import ServiceLayer.HR.WrapperService;
import ServiceLayer.Inventory.InventoryService;
import ServiceLayer.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LegacyConfig {

    private static final Logger log = LoggerFactory.getLogger(LegacyConfig.class);

    /**
     * HR wrapper: exposes hrManagerService + employeeService.
     * Also loads HR data from InventoryHR.db on startup.
     */
    @Bean
    public WrapperService wrapperService() throws ClassNotFoundException {
        WrapperService wrapper = new WrapperService();
        try {
            wrapper.loadDataFromDB();
            log.info("HR data loaded successfully from InventoryHR.db");
        } catch (Exception e) {
            log.error("Failed to load HR data from DB", e);
        }
        return wrapper;
    }

    /**
     * Inventory service: wraps InventoryFacade and triggers its initial load.
     */
    @Bean
    public InventoryService inventoryService() throws ClassNotFoundException {
        // Using simple callbacks that just log for now
        CallBack sundayCallback = msg -> log.info("Sunday callback: {}", msg);
        CallBack minAmountCallback = msg -> log.warn("Inventory callback: {}", msg);

        InventoryService inventoryService = new InventoryService(sundayCallback, minAmountCallback);

        // Load initial inventory data + check tomorrow orders
        Response res = inventoryService.loadInitialData();
        if (res.errorOccurred()) {
            log.error("Failed to load inventory initial data: {}", res.getErrorMsg());
        } else {
            log.info("Inventory initial data loaded successfully");
        }

        return inventoryService;
    }
}
