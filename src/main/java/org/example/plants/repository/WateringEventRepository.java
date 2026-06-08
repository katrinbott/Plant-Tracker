package org.example.plants.repository;

import org.example.plants.model.WateringEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WateringEventRepository extends JpaRepository<WateringEvent, Long> {

    List<WateringEvent> findByPlant_IdOrderByWateredAtDesc(Long plantId);
}
