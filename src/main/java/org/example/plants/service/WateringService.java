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

    public WateringService(WateringEventRepository wateringEventRepository,
                           PlantRepository plantRepository) {
        this.wateringEventRepository = wateringEventRepository;
        this.plantRepository = plantRepository;
    }

    public WateringEvent recordWatering(Long plantId, Integer amountMl, String note) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found"));

        WateringEvent wateringEvent = new WateringEvent();
        wateringEvent.setPlant(plant);
        wateringEvent.setWateredAt(LocalDateTime.now());
        wateringEvent.setAmountMl(amountMl);
        wateringEvent.setNote(note);

        return wateringEventRepository.save(wateringEvent);
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
