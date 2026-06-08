package org.example.plants.repository;

import org.example.plants.model.WateringEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WateringEventRepository extends JpaRepository<WateringEvent, Long> {
}
