package org.example.plants.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watering_events")
public class WateringEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plant_id")
    private Long plantId;

    @Column(name = "watered_at")
    private LocalDateTime wateredAt;

    @Column(name = "amount_ml")
    private Integer amountMl;

    private String note;

    @PrePersist
    void prePersist() {
        wateredAt = LocalDateTime.now();
    }

    // getters and setters
}