package org.example.plants.model;

import jakarta.persistence.*;

@Entity
@Table(name = "app_settings")
public class AppSettings {
    @Id
    private Integer id = 1;
    private double latitude;
    private double longitude;

    public AppSettings() {}
    public AppSettings(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Integer getId() { return id; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
