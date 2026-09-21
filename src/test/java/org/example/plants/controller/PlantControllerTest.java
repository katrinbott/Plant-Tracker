package org.example.plants.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.plants.model.Plant;
import org.example.plants.service.PlantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest spins up only the web layer (no database, no full Spring context).
// It's faster than a full integration test and good for testing HTTP behaviour:
// request mapping, validation, response codes, and JSON serialization.
@WebMvcTest(PlantController.class)
@TestPropertySource(properties = "app.base-url=http://localhost:8080")
class PlantControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // @MockitoBean replaces the real PlantService bean with a Mockito mock
    @MockitoBean PlantService plantService;

    @Test
    void createPlant_returns201AndPlantBody() throws Exception {
        Plant plant = new Plant();
        plant.setName("Monstera");
        plant.setSpecies("Monstera deliciosa");
        when(plantService.createPlant(any())).thenReturn(plant);

        mockMvc.perform(post("/plants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Monstera\", \"species\": \"Monstera deliciosa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Monstera"))
                .andExpect(jsonPath("$.species").value("Monstera deliciosa"));
    }

    @Test
    void createPlant_returns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/plants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPlants_returnsListFromService() throws Exception {
        Plant plant = new Plant();
        plant.setName("Fern");
        when(plantService.getAllPlants()).thenReturn(List.of(plant));

        mockMvc.perform(get("/plants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Fern"));
    }

    @Test
    void deletePlant_returns204() throws Exception {
        mockMvc.perform(delete("/plants/1"))
                .andExpect(status().isNoContent());
    }
}
