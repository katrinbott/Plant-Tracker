package org.example.plants.service;

import org.example.plants.dto.PlantCreateRequest;
import org.example.plants.model.Plant;
import org.example.plants.repository.PlantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlantService {
    private static final Logger log = LoggerFactory.getLogger(PlantService.class);

    private final PlantRepository plantRepository;

    public PlantService(PlantRepository plantRepository) {
        this.plantRepository = plantRepository;
    }

    public Plant createPlant(PlantCreateRequest request) {
        Plant plant = new Plant();
        plant.setName(request.name());
        plant.setSpecies(request.species());
        plant.setLocation(request.location());
        return plantRepository.save(plant);
    }

    public List<Plant> getAllPlants() {
        return plantRepository.findAll();
    }

    public void deletePlant(Long id){
        if(plantRepository.existsById(id)){
            plantRepository.deleteById(id);
            log.info("Deleted plant with id {}.", id);
        }
        else{
            log.warn("Plant with id {} unknown. Nothing to delete.", id);
        }
    }
}
