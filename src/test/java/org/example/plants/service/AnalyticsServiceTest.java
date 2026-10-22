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
        LocalDateTime now = LocalDateTime.now();
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L))
                .thenReturn(List.of(eventAt(now.minusDays(3))));

        PlantAnalytics result = analyticsService.getAnalyticsForPlant(1L);

        assertThat(result.totalWaterings()).isEqualTo(1);
        assertThat(result.averageDaysBetweenWaterings()).isNull();
        assertThat(result.daysSinceLastWatering()).isEqualTo(3L);
    }

    @Test
    void twoEvents_calculatesCorrectAverage() {
        LocalDateTime now = LocalDateTime.now();
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L)).thenReturn(List.of(
                eventAt(now.minusDays(2)),
                eventAt(now.minusDays(9))
        ));

        PlantAnalytics result = analyticsService.getAnalyticsForPlant(1L);

        assertThat(result.totalWaterings()).isEqualTo(2);
        assertThat(result.averageDaysBetweenWaterings()).isEqualTo(7.0);
        assertThat(result.daysSinceLastWatering()).isEqualTo(2L);
    }

    @Test
    void medianIgnoresAForgottenStretchThatSkewsTheAverage() {
        LocalDateTime now = LocalDateTime.now();
        // gaps: 5, 5, 5, 30 — the 30 is a stretch where the plant was forgotten
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L)).thenReturn(List.of(
                eventAt(now.minusDays(1)),
                eventAt(now.minusDays(6)),
                eventAt(now.minusDays(11)),
                eventAt(now.minusDays(16)),
                eventAt(now.minusDays(46))
        ));

        PlantAnalytics result = analyticsService.getAnalyticsForPlant(1L);

        assertThat(result.averageDaysBetweenWaterings()).isEqualTo(11.25);
        // Even number of gaps, so the median is the mean of the two middle values: 5 and 5.
        assertThat(result.medianDaysBetweenWaterings()).isEqualTo(5.0);
    }

    @Test
    void oddNumberOfGaps_medianIsTheMiddleGap() {
        LocalDateTime now = LocalDateTime.now();
        // gaps: 4, 6, 20 → median 6
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L)).thenReturn(List.of(
                eventAt(now.minusDays(2)),
                eventAt(now.minusDays(6)),
                eventAt(now.minusDays(12)),
                eventAt(now.minusDays(32))
        ));

        assertThat(analyticsService.getAnalyticsForPlant(1L).medianDaysBetweenWaterings())
                .isEqualTo(6.0);
    }

    @Test
    void twoEvents_medianEqualsTheSingleGap() {
        LocalDateTime now = LocalDateTime.now();
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L)).thenReturn(List.of(
                eventAt(now.minusDays(2)),
                eventAt(now.minusDays(9))
        ));

        assertThat(analyticsService.getAnalyticsForPlant(1L).medianDaysBetweenWaterings())
                .isEqualTo(7.0);
    }

    @Test
    void oneEvent_hasNoMedian() {
        LocalDateTime now = LocalDateTime.now();
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L))
                .thenReturn(List.of(eventAt(now.minusDays(3))));

        assertThat(analyticsService.getAnalyticsForPlant(1L).medianDaysBetweenWaterings()).isNull();
    }

    @Test
    void multipleEvents_averagesGapsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        // gaps: 3 days + 7 days → average 5.0
        when(wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(1L)).thenReturn(List.of(
                eventAt(now.minusDays(1)),
                eventAt(now.minusDays(4)),
                eventAt(now.minusDays(11))
        ));

        PlantAnalytics result = analyticsService.getAnalyticsForPlant(1L);

        assertThat(result.totalWaterings()).isEqualTo(3);
        assertThat(result.averageDaysBetweenWaterings()).isEqualTo(5.0);
    }
}
