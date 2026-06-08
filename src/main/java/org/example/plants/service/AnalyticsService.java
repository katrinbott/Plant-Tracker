package org.example.plants.service;

import org.example.plants.dto.PlantAnalytics;
import org.example.plants.model.WateringEvent;
import org.example.plants.repository.WateringEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AnalyticsService {

    private final WateringEventRepository wateringEventRepository;

    public AnalyticsService(WateringEventRepository wateringEventRepository) {
        this.wateringEventRepository = wateringEventRepository;
    }

    public PlantAnalytics getAnalyticsForPlant(Long plantId) {
        List<WateringEvent> wateringEvents = wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(plantId);

        if (wateringEvents.isEmpty()) {
            return new PlantAnalytics(0, null, null);
        }

        long daysSinceLast = ChronoUnit.DAYS.between(wateringEvents.get(0).getWateredAt(), LocalDateTime.now());

        if (wateringEvents.size() < 2) {
            return new PlantAnalytics(1, null, daysSinceLast);
        }

        double totalDays = 0;
        for (int i = 0; i < wateringEvents.size() - 1; i++) {
            totalDays += ChronoUnit.DAYS.between(wateringEvents.get(i + 1).getWateredAt(), wateringEvents.get(i).getWateredAt());
        }
        double average = totalDays / (wateringEvents.size() - 1);

        return new PlantAnalytics(wateringEvents.size(), average, daysSinceLast);
    }
}

