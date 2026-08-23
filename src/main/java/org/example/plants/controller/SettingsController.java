package org.example.plants.controller;

import org.example.plants.repository.AppSettingsRepository;
import org.example.plants.service.WeatherService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final WeatherService weatherService;
    private final AppSettingsRepository appSettingsRepository;

    public SettingsController(WeatherService weatherService, AppSettingsRepository appSettingsRepository) {
        this.weatherService = weatherService;
        this.appSettingsRepository = appSettingsRepository;
    }

    @GetMapping("/location")
    public LocationResponse getLocation() {
        boolean confirmed = appSettingsRepository.existsById(1);
        return new LocationResponse(weatherService.getLatitude(), weatherService.getLongitude(), confirmed);
    }

    @PostMapping("/location")
    public LocationResponse setLocation(@RequestBody LocationRequest request) {
        weatherService.setLocation(request.latitude(), request.longitude());
        return new LocationResponse(weatherService.getLatitude(), weatherService.getLongitude(), true);
    }

    record LocationRequest(double latitude, double longitude) {}
    record LocationResponse(double latitude, double longitude, boolean confirmed) {}
}
