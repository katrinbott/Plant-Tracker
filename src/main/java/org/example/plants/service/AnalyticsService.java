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
            return new PlantAnalytics(0, null, null, null, false);
        }

        long daysSinceLast = ChronoUnit.DAYS.between(wateringEvents.get(0).getWateredAt().toLocalDate(), LocalDate.now());

        // Several waterings on one day count as one: a same-day top-up would otherwise
        // add a zero gap and drag the median down.
        List<LocalDate> dates = wateringEvents.stream()
                .map(event -> event.getWateredAt().toLocalDate())
                .distinct()
                .toList();

        if (dates.size() < 2) {
            return new PlantAnalytics(wateringEvents.size(), null, null, daysSinceLast, false);
        }

        // Dates arrive newest-first, so each gap is the older subtracted from the newer.
        List<Long> gaps = new ArrayList<>();
        for (int i = 0; i < dates.size() - 1; i++) {
            gaps.add(ChronoUnit.DAYS.between(dates.get(i + 1), dates.get(i)));
        }

        double average = gaps.stream().mapToLong(Long::longValue).average().orElseThrow();
        double median = Medians.of(gaps.stream().map(Long::doubleValue).toList());

        // Due is measured against the median, not the mean: one forgotten stretch would
        // otherwise raise the bar and hide a plant that is genuinely due.
        boolean overdue = daysSinceLast > median;

        return new PlantAnalytics(wateringEvents.size(), average, median, daysSinceLast, overdue);
    }
}

