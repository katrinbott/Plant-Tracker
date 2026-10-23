package org.example.plants.dto;

import java.util.List;

public record RoomStats(
        Long roomId,
        String name,
        int plantCount,
        Long daysSinceLastWatering,
        Double averageDaysBetweenWaterings,
        List<RoomPlant> plants) {}
