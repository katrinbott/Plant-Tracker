package org.example.plants.dto;

public record PlantAnalytics(
        int totalWaterings,
        Double averageDaysBetweenWaterings,
        Double medianDaysBetweenWaterings,
        Long daysSinceLastWatering
) {}
