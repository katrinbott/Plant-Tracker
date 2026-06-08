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

    public Long getId() {
        return id;
    }

    @JsonIgnore
    public Plant getPlant() {
        return plant;
    }

    public void setPlant(Plant plant) {
        this.plant = plant;
    }

    public Long getPlantId() {
        return plant != null ? plant.getId() : null;
    }

    public LocalDateTime getWateredAt() {
        return wateredAt;
    }

    public void setWateredAt(LocalDateTime wateredAt) {
        this.wateredAt = wateredAt;
    }

    public Integer getAmountMl() {
        return amountMl;
    }

    public void setAmountMl(Integer amountMl) {
        this.amountMl = amountMl;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}