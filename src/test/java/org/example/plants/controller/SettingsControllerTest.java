package org.example.plants.controller;

import org.example.plants.repository.AppSettingsRepository;
import org.example.plants.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
class SettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean WeatherService weatherService;
    @MockitoBean AppSettingsRepository appSettingsRepository;

    @Test
    void getLocation_returnsCurrentCoordinatesAndConfirmedFlag() throws Exception {
        when(weatherService.getLatitude()).thenReturn(53.5511);
        when(weatherService.getLongitude()).thenReturn(9.9937);
        when(appSettingsRepository.existsById(1)).thenReturn(true);

        mockMvc.perform(get("/settings/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(53.5511))
                .andExpect(jsonPath("$.longitude").value(9.9937))
                .andExpect(jsonPath("$.confirmed").value(true));
    }

    @Test
    void getLocation_confirmedFalseWhenNotSavedInDb() throws Exception {
        when(weatherService.getLatitude()).thenReturn(52.52437);
        when(weatherService.getLongitude()).thenReturn(13.41053);
        when(appSettingsRepository.existsById(1)).thenReturn(false);

        mockMvc.perform(get("/settings/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(false));
    }

    @Test
    void setLocation_updatesServiceAndReturnsConfirmedTrue() throws Exception {
        when(weatherService.getLatitude()).thenReturn(48.1351);
        when(weatherService.getLongitude()).thenReturn(11.5820);

        mockMvc.perform(post("/settings/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\": 48.1351, \"longitude\": 11.5820}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true));

        verify(weatherService).setLocation(48.1351, 11.5820);
    }
}
