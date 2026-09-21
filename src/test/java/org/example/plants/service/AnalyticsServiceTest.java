package org.example.plants.service;

import org.example.plants.dto.PlantAnalytics;
import org.example.plants.model.WateringEvent;
import org.example.plants.repository.WateringEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    WateringEventRepository wateringEventRepository;

    @InjectMocks
    AnalyticsService analyticsService;

    private WateringEvent eventAt(LocalDateTime time) {
        WateringEvent e = new WateringEvent();
        e.setWateredAt(time);
        return e;
    }

    @Test
    void noEvents_returnsZeroTotalAndNulls() {
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L)).thenReturn(List.of());

        PlantAnalytics result = analyticsService.getAnalyticsForPlant(1L);

        assertThat(result.totalWaterings()).isEqualTo(0);
        assertThat(result.averageDaysBetweenWaterings()).isNull();
        assertThat(result.daysSinceLastWatering()).isNull();
    }

    @Test
    void oneEvent_returnsNullAverageAndCorrectDaysSinceLast() {
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L))
                .thenReturn(List.of(eventAt(LocalDateTime.now().minusDays(3))));

        PlantAnalytics result = analyticsService.getAnalyticsForPlant(1L);

        assertThat(result.totalWaterings()).isEqualTo(1);
        assertThat(result.averageDaysBetweenWaterings()).isNull();
        assertThat(result.daysSinceLastWatering()).isEqualTo(3L);
    }

    @Test
    void twoEvents_calculatesCorrectAverage() {
        // newest first — gap is 7 days
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L)).thenReturn(List.of(
                eventAt(LocalDateTime.now().minusDays(2)),
                eventAt(LocalDateTime.now().minusDays(9))
        ));

        PlantAnalytics result = analyticsService.getAnalyticsForPlant(1L);

        assertThat(result.totalWaterings()).isEqualTo(2);
        assertThat(result.averageDaysBetweenWaterings()).isEqualTo(7.0);
        assertThat(result.daysSinceLastWatering()).isEqualTo(2L);
    }

    @Test
    void multipleEvents_averagesGapsCorrectly() {
        // gaps: 3 days + 7 days → average 5.0
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L)).thenReturn(List.of(
                eventAt(LocalDateTime.now().minusDays(1)),
                eventAt(LocalDateTime.now().minusDays(4)),
                eventAt(LocalDateTime.now().minusDays(11))
        ));

        PlantAnalytics result = analyticsService.getAnalyticsForPlant(1L);

        assertThat(result.totalWaterings()).isEqualTo(3);
        assertThat(result.averageDaysBetweenWaterings()).isEqualTo(5.0);
    }
}
