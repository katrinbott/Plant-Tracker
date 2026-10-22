package org.example.plants.repository;
import org.example.plants.model.Plant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlantRepository extends JpaRepository<Plant, Long> {
    List<Plant> findByRoom_Id(Long roomId);
    long countByRoom_Id(Long roomId);
}
