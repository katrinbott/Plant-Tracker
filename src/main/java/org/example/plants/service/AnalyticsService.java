package org.example.plants.service;

import org.example.plants.dto.PlantAnalytics;
import org.example.plants.model.WateringEvent;
import org.example.plants.repository.WateringEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
            return new PlantAnalytics(0, null, null, null);
        }

        long daysSinceLast = ChronoUnit.DAYS.between(wateringEvents.get(0).getWateredAt().toLocalDate(), LocalDate.now());

        if (wateringEvents.size() < 2) {
            return new PlantAnalytics(1, null, null, daysSinceLast);
        }

        // Events arrive newest-first, so each gap is the older date subtracted from the
        // newer one. Calendar days, so the time of day cannot shift a gap.
        List<Long> gaps = new ArrayList<>();
        for (int i = 0; i < wateringEvents.size() - 1; i++) {
            gaps.add(ChronoUnit.DAYS.between(wateringEvents.get(i + 1).getWateredAt().toLocalDate(),
                    wateringEvents.get(i).getWateredAt().toLocalDate()));
        }

        double average = gaps.stream().mapToLong(Long::longValue).average().orElseThrow();

        return new PlantAnalytics(wateringEvents.size(), average, median(gaps), daysSinceLast);
    }

    /**
     * The median gap is what tells you whether a plant is due: one stretch where it was
     * forgotten drags the mean up and would make a thirsty plant look fine.
     */
    private double median(List<Long> gaps) {
        List<Long> sorted = gaps.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1
                ? sorted.get(middle)
                : (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }
}

