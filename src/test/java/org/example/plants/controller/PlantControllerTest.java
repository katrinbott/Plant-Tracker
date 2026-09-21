package org.example.plants.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.plants.model.Plant;
import org.example.plants.model.PlantImage;
import org.example.plants.service.PlantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    @Test
    void uploadImage_returns201WithImageBody() throws Exception {
        PlantImage image = new PlantImage();
        image.setPlantId(1L);
        image.setNote("looking good");
        when(plantService.addImage(eq(1L), any(), eq("looking good"))).thenReturn(image);

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/plants/1/images").file(file).param("note", "looking good"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plantId").value(1))
                .andExpect(jsonPath("$.note").value("looking good"));
    }

    @Test
    void uploadImage_returns201WithoutNote() throws Exception {
        PlantImage image = new PlantImage();
        image.setPlantId(1L);
        when(plantService.addImage(eq(1L), any(), isNull())).thenReturn(image);

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/plants/1/images").file(file))
                .andExpect(status().isCreated());
    }

    @Test
    void getImages_returnsListForPlant() throws Exception {
        PlantImage image = new PlantImage();
        image.setPlantId(1L);
        when(plantService.getImages(1L)).thenReturn(List.of(image));

        mockMvc.perform(get("/plants/1/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plantId").value(1));
    }

    @Test
    void deleteImage_returns204() throws Exception {
        when(plantService.deleteImage(1L, 5L)).thenReturn(true);

        mockMvc.perform(delete("/plants/1/images/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteImage_returns404WhenNotFound() throws Exception {
        when(plantService.deleteImage(1L, 99L)).thenReturn(false);

        mockMvc.perform(delete("/plants/1/images/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getImageBytes_returnsImageWithCorrectContentType() throws Exception {
        byte[] imageBytes = new byte[]{(byte) 0xFF, (byte) 0xD8}; // JPEG magic bytes
        PlantImage image = new PlantImage();
        image.setId(5L);
        image.setPlantId(1L);
        image.setImagePath("photo.jpg");
        when(plantService.getImageBytes(5L)).thenReturn(imageBytes);
        when(plantService.getImages(1L)).thenReturn(List.of(image));

        mockMvc.perform(get("/plants/1/images/5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
    }

    @Test
    void getImageBytes_returns404WhenNotFound() throws Exception {
        when(plantService.getImageBytes(99L)).thenReturn(null);

        mockMvc.perform(get("/plants/1/images/99"))
                .andExpect(status().isNotFound());
    }
}
