package org.example.plants.controller;

import jakarta.validation.Valid;
import org.example.plants.dto.PlantCreateRequest;
import org.example.plants.model.Plant;
import org.example.plants.service.PlantService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plants")
public class PlantController {

    private final PlantService plantService;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Plant createPlant(@RequestBody @Valid PlantCreateRequest request) {
        return plantService.createPlant(request);
    }

    @GetMapping
    public List<Plant> getAllPlants() {
        return plantService.getAllPlants();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlant(@PathVariable Long id) {
        plantService.deletePlant(id);
    }
}