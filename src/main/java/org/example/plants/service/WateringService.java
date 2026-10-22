package org.example.plants.service;

import org.example.plants.model.Plant;
import org.example.plants.model.WateringEvent;
import org.example.plants.repository.PlantRepository;
import org.example.plants.repository.WateringEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WateringService {
    private static final Logger log = LoggerFactory.getLogger(WateringService.class);

    private final WateringEventRepository wateringEventRepository;
    private final PlantRepository plantRepository;
    private final WeatherService weatherService;

    public WateringService(WateringEventRepository wateringEventRepository,
                           PlantRepository plantRepository,
                           WeatherService weatherService) {
        this.wateringEventRepository = wateringEventRepository;
        this.plantRepository = plantRepository;
        this.weatherService = weatherService;
    }

    public WateringEvent recordWatering(Long plantId, Integer amountMl, String note, boolean fertilizer) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found"));

        return wateringEventRepository.save(
                buildEvent(plant, amountMl, note, fertilizer, weatherService.fetchCurrent()));
    }

    /**
     * Records one event per plant, with no amount: a round rarely gives every plant the
     * same volume, so amounts belong on single events. Returns null when any id is
     * unknown, so watering a room is all or nothing rather than silently skipping
     * plants. Duplicate ids are rejected the same way, which stops a plant being
     * watered twice in one round.
     */
    public List<WateringEvent> recordWateringForPlants(List<Long> plantIds, String note,
                                                       boolean fertilizer) {
        List<Plant> plants = plantRepository.findAllById(plantIds);
        if (plants.size() != plantIds.size()) return null;

        // One weather lookup for the whole round: a room is watered in a single go, so
        // per-plant calls would hit the API repeatedly and stamp differing readings.
        WeatherService.WeatherData weather = weatherService.fetchCurrent();

        return wateringEventRepository.saveAll(plants.stream()
                .map(plant -> buildEvent(plant, null, note, fertilizer, weather))
                .toList());
    }

    private WateringEvent buildEvent(Plant plant, Integer amountMl, String note,
                                     boolean fertilizer, WeatherService.WeatherData weather) {
        WateringEvent wateringEvent = new WateringEvent();
        wateringEvent.setPlant(plant);
        wateringEvent.setWateredAt(LocalDateTime.now());
        wateringEvent.setAmountMl(amountMl);
        wateringEvent.setNote(note);
        wateringEvent.setFertilizer(fertilizer);

        if (weather != null) {
            wateringEvent.setTemperatureC(weather.temperatureC());
            wateringEvent.setHumidityPercent(weather.humidityPercent());
            wateringEvent.setWeatherCode(weather.weatherCode());
            wateringEvent.setMinTemperatureC(weather.minTemperatureC());
            wateringEvent.setMaxTemperatureC(weather.maxTemperatureC());
        }

        return wateringEvent;
    }

    public List<WateringEvent> getAllWateringEvents() {
        return wateringEventRepository.findAll();
    }

    public List<WateringEvent> getWateringHistoryForPlant(Long plantId) {
        return wateringEventRepository.findByPlant_IdOrderByWateredAtDesc(plantId);
    }

    public void deleteWateringEvent(Long id) {

        if(wateringEventRepository.existsById(id)){
            wateringEventRepository.deleteById(id);
            log.info("Deleted watering event with id {}.", id);
        }
        else{
            log.warn("Watering event with id {} unknown. Nothing to delete.", id);
        }
    }

}
