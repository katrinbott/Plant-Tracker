package org.example.plants.controller;

import jakarta.validation.Valid;
import org.example.plants.dto.BulkWateringRequest;
import org.example.plants.dto.WaterCreateRequest;
import org.example.plants.model.WateringEvent;
import org.example.plants.service.PlantService;
import org.example.plants.service.WateringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/watering")
public class WateringController {

    private final WateringService wateringService;

    public WateringController(WateringService wateringService) {
        this.wateringService = wateringService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WateringEvent recordWatering(@RequestBody @Valid WaterCreateRequest.WateringRequest wateringRequest) {
        return wateringService.recordWatering(wateringRequest.plantId(), wateringRequest.amountMl(),
                wateringRequest.note(), wateringRequest.fertilizer());
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<WateringEvent>> recordWateringForPlants(
            @RequestBody @Valid BulkWateringRequest request) {
        List<WateringEvent> events = wateringService.recordWateringForPlants(
                request.plantIds(), request.note(), request.fertilizer());
        return events != null
                ? ResponseEntity.status(HttpStatus.CREATED).body(events)
                : ResponseEntity.badRequest().build();
    }

    @GetMapping
    public List<WateringEvent> getAllWateringEvents() {
        return wateringService.getAllWateringEvents();
    }

    @GetMapping("/plant/{plantId}")
    public List<WateringEvent> getWateringHistoryForPlant(@PathVariable Long plantId) {
        return wateringService.getWateringHistoryForPlant(plantId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWateringEvent(@PathVariable Long id) {
        wateringService.deleteWateringEvent(id);
    }
}
