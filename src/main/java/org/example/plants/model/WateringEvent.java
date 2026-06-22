package org.example.plants.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "watering_events")
public class WateringEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @Column(name = "watered_at")
    private LocalDateTime wateredAt;

    @Column(name = "amount_ml")
    private Integer amountMl;

    private String note;

    @Column(name = "temperature_c")
    private Double temperatureC;

    @Column(name = "min_temperature_c")
    private Double minTemperatureC;

    @Column(name = "max_temperature_c")
    private Double maxTemperatureC;

    @Column(name = "humidity_percent")
    private Double humidityPercent;

    @Column(name = "weather_code")
    private Integer weatherCode;

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
    public Double getTemperatureC() { return temperatureC; }

    public void setTemperatureC(Double temperatureC) { this.temperatureC = temperatureC; }

    public Double getMinTemperatureC() { return minTemperatureC;}

    public void setMinTemperatureC(Double minTemperatureC) {
        this.minTemperatureC = minTemperatureC;
    }

    public Double getMaxTemperatureC() { return maxTemperatureC;}

    public void setMaxTemperatureC(Double maxTemperatureC) { this.maxTemperatureC = maxTemperatureC; }

    public Double getHumidityPercent() {
        return humidityPercent;
    }

    public void setHumidityPercent(Double humidityPercent) {
        this.humidityPercent = humidityPercent;
    }

    public Integer getWeatherCode() {
        return weatherCode;
    }

    public void setWeatherCode(Integer weatherCode) {
        this.weatherCode = weatherCode;
    }
}