package org.example.plants.service;

import org.example.plants.dto.PlantCreateRequest;
import org.example.plants.model.Plant;
import org.example.plants.model.PlantImage;
import org.example.plants.model.Room;
import org.example.plants.repository.PlantImageRepository;
import org.example.plants.repository.PlantRepository;
import org.example.plants.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class PlantService {
    private static final Logger log = LoggerFactory.getLogger(PlantService.class);

    private final PlantRepository plantRepository;
    private final PlantImageRepository plantImageRepository;
    private final RoomRepository roomRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public PlantService(PlantRepository plantRepository, PlantImageRepository plantImageRepository,
                        RoomRepository roomRepository) {
        this.plantRepository = plantRepository;
        this.plantImageRepository = plantImageRepository;
        this.roomRepository = roomRepository;
    }

    /** Returns null when the requested room does not exist; every plant needs one. */
    public Plant createPlant(PlantCreateRequest request) {
        Room room = roomRepository.findById(request.roomId()).orElse(null);
        if (room == null) return null;
        Plant plant = new Plant();
        plant.setName(request.name());
        plant.setSpecies(request.species());
        plant.setLocation(request.location());
        plant.setRoom(room);
        return plantRepository.save(plant);
    }

    /** Returns null when either the plant or the target room does not exist. */
    public Plant setRoom(Long plantId, Long roomId) {
        Plant plant = plantRepository.findById(plantId).orElse(null);
        Room room = roomRepository.findById(roomId).orElse(null);
        if (plant == null || room == null) return null;
        plant.setRoom(room);
        return plantRepository.save(plant);
    }

    public List<Plant> getAllPlants() {
        return plantRepository.findAll();
    }

    public Plant getPlantById(Long id) {
        return plantRepository.findById(id).orElse(null);
    }

    public Plant updateLocation(Long id, String location) {
        Plant plant = plantRepository.findById(id).orElse(null);
        if (plant == null) return null;
        plant.setLocation(location == null || location.isBlank() ? null : location.trim());
        return plantRepository.save(plant);
    }

    public PlantImage addImage(Long plantId, MultipartFile file, String note) throws IOException {
        if (!plantRepository.existsById(plantId))
            throw new RuntimeException("Plant not found");
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + (ext != null ? "." + ext : "");
        Path filePath = dir.resolve(filename);
        file.transferTo(filePath);
        PlantImage image = new PlantImage();
        image.setPlantId(plantId);
        image.setImagePath(filePath.toString());
        if (note != null && !note.isBlank()) image.setNote(note);
        return plantImageRepository.save(image);
    }

    public List<PlantImage> getImages(Long plantId) {
        return plantImageRepository.findByPlantIdOrderByCreatedAtAsc(plantId);
    }

    public byte[] getImageBytes(Long imageId) throws IOException {
        PlantImage image = plantImageRepository.findById(imageId).orElse(null);
        if (image == null) return null;
        Path path = Paths.get(image.getImagePath());
        if (!Files.exists(path)) return null;
        return Files.readAllBytes(path);
    }

    public boolean deleteImage(Long plantId, Long imageId) throws IOException {
        if (!plantImageRepository.existsByIdAndPlantId(imageId, plantId)) return false;
        PlantImage image = plantImageRepository.findById(imageId).orElseThrow();
        Files.deleteIfExists(Paths.get(image.getImagePath()));
        plantImageRepository.deleteById(imageId);
        return true;
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
