package org.example.plants.controller;

import org.example.plants.dto.PlantAnalytics;
import org.example.plants.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AnalyticsService analyticsService;

    @Test
    void getAnalytics_returnsCorrectJson() throws Exception {
        when(analyticsService.getAnalyticsForPlant(1L))
                .thenReturn(new PlantAnalytics(5, 3.5, 2L));

        mockMvc.perform(get("/analytics/plant/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWaterings").value(5))
                .andExpect(jsonPath("$.averageDaysBetweenWaterings").value(3.5))
                .andExpect(jsonPath("$.daysSinceLastWatering").value(2));
    }

    @Test
    void getAnalytics_returnsNullsWhenNoEvents() throws Exception {
        when(analyticsService.getAnalyticsForPlant(1L))
                .thenReturn(new PlantAnalytics(0, null, null));

        mockMvc.perform(get("/analytics/plant/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWaterings").value(0))
                .andExpect(jsonPath("$.averageDaysBetweenWaterings").isEmpty())
                .andExpect(jsonPath("$.daysSinceLastWatering").isEmpty());
    }
}
