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

        assertThatThrownBy(() -> wateringService.recordWatering(99L, 200, "note"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Plant not found");
    }

    @Test
    void recordWatering_attachesWeatherDataWhenAvailable() {
        when(plantRepository.findById(1L)).thenReturn(Optional.of(new Plant()));
        when(weatherService.fetchCurrent()).thenReturn(
                new WeatherService.WeatherData(20.5, 65.0, 1, 15.0, 25.0));
        when(wateringEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WateringEvent result = wateringService.recordWatering(1L, 200, "looked dry");

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

        WateringEvent result = wateringService.recordWatering(1L, 100, null);

        assertThat(result.getTemperatureC()).isNull();
        assertThat(result.getAmountMl()).isEqualTo(100);
        verify(wateringEventRepository).save(any());
    }

    @Test
    void deleteWateringEvent_doesNothingWhenEventNotFound() {
        when(wateringEventRepository.existsById(99L)).thenReturn(false);

        wateringService.deleteWateringEvent(99L);

        verify(wateringEventRepository, never()).deleteById(any());
    }
}
