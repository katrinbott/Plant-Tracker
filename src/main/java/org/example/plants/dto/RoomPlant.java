package org.example.plants.dto;

/** A plant as the Rooms tab shows it. Both numbers are null until it has two waterings. */
public record RoomPlant(
        Long plantId,
        String name,
        Long daysSinceLastWatering,
        Double medianDaysBetweenWaterings,
        boolean overdue) {}
