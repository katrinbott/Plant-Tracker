package org.example.plants.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    @Value("${app.weather.latitude}")
    private double latitude;

    @Value("${app.weather.longitude}")
    private double longitude;

    private final RestClient restClient;

    public WeatherService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public WeatherData fetchCurrent() {
        String url = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&daily=temperature_2m_max,temperature_2m_min&current=temperature_2m,relative_humidity_2m,weather_code&timezone=auto&forecast_days=1",
            latitude, longitude
        );
        try {
            OpenMeteoResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(OpenMeteoResponse.class);
            if (response == null || response.current() == null || response.daily() == null) return null;
            CurrentWeather c = response.current();
            DailyWeather d = response.daily();
            return new WeatherData(
                c.temperature2m(),
                c.relativeHumidity2m(),
                c.weatherCode(),
                d.temperature2mMin() != null && !d.temperature2mMin().isEmpty() ? d.temperature2mMin().get(0) : null,
                d.temperature2mMax() != null && !d.temperature2mMax().isEmpty() ? d.temperature2mMax().get(0) : null
            );
        } catch (Exception e) {
            log.warn("Could not fetch weather data: {}", e.getMessage());
            return null;
        }
    }

    public record WeatherData(Double temperatureC, Double humidityPercent, Integer weatherCode, Double minTemperatureC, Double maxTemperatureC) {}

    private record OpenMeteoResponse(CurrentWeather current, DailyWeather daily) {}

    private record CurrentWeather(
            @JsonProperty("temperature_2m")       Double temperature2m,
            @JsonProperty("relative_humidity_2m") Double relativeHumidity2m,
            @JsonProperty("weather_code")         Integer weatherCode
    ) {}

    private record DailyWeather(
            @JsonProperty("temperature_2m_min") List<Double> temperature2mMin,
            @JsonProperty("temperature_2m_max") List<Double> temperature2mMax
    ) {}
}
