package org.example.plants.dto;

public record OverduePlant(
        Long plantId,
        String name,
        long daysSinceLastWatering,
        double medianDaysBetweenWaterings) {}
