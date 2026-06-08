package org.example.plants.controller;

import org.example.plants.dto.PlantAnalytics;
import org.example.plants.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/plant/{plantId}")
    public PlantAnalytics getAnalyticsForPlant(@PathVariable Long plantId) {
        return analyticsService.getAnalyticsForPlant(plantId);
    }
}
