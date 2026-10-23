package org.example.plants.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.plants.exception.RoomNotFoundException;
import org.example.plants.model.Plant;
import org.example.plants.model.PlantImage;
import org.example.plants.model.Room;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        plant.setRoom(roomWithId(2L, "Living room"));
        when(plantService.createPlant(any())).thenReturn(plant);

        mockMvc.perform(post("/plants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Monstera\", \"species\": \"Monstera deliciosa\", \"roomId\": 2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Monstera"))
                .andExpect(jsonPath("$.species").value("Monstera deliciosa"))
                .andExpect(jsonPath("$.roomId").value(2))
                .andExpect(jsonPath("$.room").doesNotExist());
    }

    @Test
    void createPlant_returns400WhenNameIsBlank() throws Exception {
        mockMvc.perform(post("/plants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"roomId\": 2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlant_returns400WhenRoomIdMissing() throws Exception {
        mockMvc.perform(post("/plants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Monstera\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlant_returns400WhenRoomDoesNotExist() throws Exception {
        when(plantService.createPlant(any())).thenThrow(new RoomNotFoundException(99L));

        mockMvc.perform(post("/plants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Monstera\", \"roomId\": 99}"))
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
    void updateLocation_returns200WithUpdatedPlant() throws Exception {
        Plant plant = new Plant();
        plant.setName("Fern");
        plant.setLocation("Balcony");
        when(plantService.updateLocation(1L, "Balcony")).thenReturn(plant);

        mockMvc.perform(patch("/plants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\": \"Balcony\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Balcony"));
    }

    @Test
    void updateLocation_returns404WhenPlantNotFound() throws Exception {
        when(plantService.updateLocation(eq(99L), any())).thenReturn(null);

        mockMvc.perform(patch("/plants/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\": \"Balcony\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void setRoom_returns200WithUpdatedPlant() throws Exception {
        Plant plant = new Plant();
        plant.setName("Fern");
        plant.setRoom(roomWithId(3L, "Kitchen"));
        when(plantService.setRoom(1L, 3L)).thenReturn(plant);

        mockMvc.perform(put("/plants/1/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fern"))
                .andExpect(jsonPath("$.roomId").value(3))
                .andExpect(jsonPath("$.room").doesNotExist());
    }

    @Test
    void setRoom_returns400WhenRoomIdIsNull() throws Exception {
        mockMvc.perform(put("/plants/1/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\": null}"))
                .andExpect(status().isBadRequest());

        verify(plantService, never()).setRoom(any(), any());
    }

    @Test
    void setRoom_returns404WhenPlantNotFound() throws Exception {
        when(plantService.setRoom(eq(99L), any())).thenReturn(null);

        mockMvc.perform(put("/plants/99/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\": 3}"))
                .andExpect(status().isNotFound());
    }

    // The URL names a plant that exists and the body names a room that does not, so
    // this is a bad request rather than the 404 above.
    @Test
    void setRoom_returns400WhenRoomDoesNotExist() throws Exception {
        when(plantService.setRoom(eq(1L), any())).thenThrow(new RoomNotFoundException(99L));

        mockMvc.perform(put("/plants/1/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomId\": 99}"))
                .andExpect(status().isBadRequest());
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

    // A plant serializes its room as a bare roomId, which the frontend reads. The
    // association itself must stay out of the JSON: it is lazily fetched, so with
    // open-in-view off Jackson would walk into a detached proxy and fail at runtime.
    // Nothing here is a proxy, so only asserting roomId would let that regression through.
    private static Room roomWithId(Long id, String name) {
        Room room = new Room(name);
        room.setId(id);
        return room;
    }
}
