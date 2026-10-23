package org.example.plants.service;

import org.example.plants.model.Plant;
import org.example.plants.model.WateringEvent;
import org.example.plants.repository.PlantRepository;
import org.example.plants.repository.WateringEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WateringServiceTest {

    @Mock WateringEventRepository wateringEventRepository;
    @Mock PlantRepository plantRepository;
    @Mock WeatherService weatherService;
    @InjectMocks WateringService wateringService;

    @Test
    void recordWatering_throwsWhenPlantNotFound() {
        when(plantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wateringService.recordWatering(99L, 200, "note", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Plant not found");
    }

    @Test
    void recordWatering_attachesWeatherDataWhenAvailable() {
        when(plantRepository.findById(1L)).thenReturn(Optional.of(new Plant()));
        when(weatherService.fetchCurrent()).thenReturn(
                new WeatherService.WeatherData(20.5, 65.0, 1, 15.0, 25.0));
        when(wateringEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WateringEvent result = wateringService.recordWatering(1L, 200, "looked dry", false);

        assertThat(result.getTemperatureC()).isEqualTo(20.5);
        assertThat(result.getHumidityPercent()).isEqualTo(65.0);
        assertThat(result.getWeatherCode()).isEqualTo(1);
        assertThat(result.getMinTemperatureC()).isEqualTo(15.0);
        assertThat(result.getMaxTemperatureC()).isEqualTo(25.0);
        assertThat(result.getAmountMl()).isEqualTo(200);
        assertThat(result.getNote()).isEqualTo("looked dry");
    }

    @Test
    void recordWatering_savesEventWithNullWeatherWhenServiceReturnsNull() {
        when(plantRepository.findById(1L)).thenReturn(Optional.of(new Plant()));
        when(weatherService.fetchCurrent()).thenReturn(null);
        when(wateringEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WateringEvent result = wateringService.recordWatering(1L, 100, null, false);

        assertThat(result.getTemperatureC()).isNull();
        assertThat(result.getAmountMl()).isEqualTo(100);
        verify(wateringEventRepository).save(any());
    }

    @Test
    void recordWatering_savesFertilizerFlagWhenSet() {
        when(plantRepository.findById(1L)).thenReturn(Optional.of(new Plant()));
        when(weatherService.fetchCurrent()).thenReturn(null);
        when(wateringEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WateringEvent result = wateringService.recordWatering(1L, 200, null, true);

        assertThat(result.isFertilizer()).isTrue();
    }

    @Test
    void recordWatering_defaultsFertilizerToFalse() {
        when(plantRepository.findById(1L)).thenReturn(Optional.of(new Plant()));
        when(weatherService.fetchCurrent()).thenReturn(null);
        when(wateringEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WateringEvent result = wateringService.recordWatering(1L, 200, null, false);

        assertThat(result.isFertilizer()).isFalse();
    }

    @Test
    void recordWateringForPlants_createsOneEventPerPlantAndFetchesWeatherOnce() {
        when(plantRepository.findAllById(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(new Plant(), new Plant(), new Plant()));
        when(weatherService.fetchCurrent()).thenReturn(
                new WeatherService.WeatherData(20.5, 65.0, 1, 15.0, 25.0));
        when(wateringEventRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<WateringEvent> result =
                wateringService.recordWateringForPlants(List.of(1L, 2L, 3L), "weekly round", true);

        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(event -> {
            // Amounts differ per plant, so a round never records one.
            assertThat(event.getAmountMl()).isNull();
            assertThat(event.getNote()).isEqualTo("weekly round");
            assertThat(event.isFertilizer()).isTrue();
            assertThat(event.getTemperatureC()).isEqualTo(20.5);
        });
        // One round of watering is one weather lookup, not one per plant.
        verify(weatherService, times(1)).fetchCurrent();
    }

    // Duplicates are rejected by the same size comparison as unknown ids, because
    // findAllById queries WHERE id IN (...) and so returns one row per distinct id.
    // With the repository mocked this pins the service's half of that: fewer rows back
    // than ids asked for means reject.
    @Test
    void recordWateringForPlants_returnsNullWhenAnIdIsDuplicated() {
        when(plantRepository.findAllById(List.of(1L, 1L))).thenReturn(List.of(new Plant()));

        List<WateringEvent> result =
                wateringService.recordWateringForPlants(List.of(1L, 1L), null, false);

        assertThat(result).isNull();
        verify(wateringEventRepository, never()).saveAll(any());
        verify(weatherService, never()).fetchCurrent();
    }

    @Test
    void recordWateringForPlants_returnsNullWhenAnyPlantIsUnknown() {
        when(plantRepository.findAllById(List.of(1L, 99L))).thenReturn(List.of(new Plant()));

        List<WateringEvent> result =
                wateringService.recordWateringForPlants(List.of(1L, 99L), null, false);

        assertThat(result).isNull();
        verify(wateringEventRepository, never()).saveAll(any());
        verify(weatherService, never()).fetchCurrent();
    }

    @Test
    void deleteWateringEvent_doesNothingWhenEventNotFound() {
        when(wateringEventRepository.existsById(99L)).thenReturn(false);

        wateringService.deleteWateringEvent(99L);

        verify(wateringEventRepository, never()).deleteById(any());
    }
}
