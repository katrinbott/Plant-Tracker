package org.example.plants.repository;

import org.example.plants.model.PlantImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantImageRepository extends JpaRepository<PlantImage, Long> {
    List<PlantImage> findByPlantIdOrderByCreatedAtAsc(Long plantId);
    boolean existsByIdAndPlantId(Long id, Long plantId);
}
