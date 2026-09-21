package org.example.plants.service;

import org.example.plants.dto.PlantCreateRequest;
import org.example.plants.model.Plant;
import org.example.plants.model.PlantImage;
import org.example.plants.repository.PlantImageRepository;
import org.example.plants.repository.PlantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantServiceTest {

    @Mock PlantRepository plantRepository;
    @Mock PlantImageRepository plantImageRepository;
    @InjectMocks PlantService plantService;

    // JUnit creates a real temporary directory for each test and deletes it afterwards
    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        // Simulate the @Value injection that Spring would normally do
        ReflectionTestUtils.setField(plantService, "uploadDir", tempDir.toString());
    }

    @Test
    void createPlant_persistsAllFields() {
        when(plantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Plant result = plantService.createPlant(new PlantCreateRequest("Monstera", "Monstera deliciosa", "Living room"));

        assertThat(result.getName()).isEqualTo("Monstera");
        assertThat(result.getSpecies()).isEqualTo("Monstera deliciosa");
        assertThat(result.getLocation()).isEqualTo("Living room");
    }

    @Test
    void addImage_savesImageWithNote() throws IOException {
        when(plantRepository.existsById(1L)).thenReturn(true);
        when(plantImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        PlantImage result = plantService.addImage(1L, file, "chopped back due to pests");

        assertThat(result.getPlantId()).isEqualTo(1L);
        assertThat(result.getNote()).isEqualTo("chopped back due to pests");
        assertThat(result.getImagePath()).endsWith(".jpg");
    }

    @Test
    void addImage_doesNotSetNoteWhenBlank() throws IOException {
        when(plantRepository.existsById(1L)).thenReturn(true);
        when(plantImageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1});

        PlantImage result = plantService.addImage(1L, file, "   ");

        assertThat(result.getNote()).isNull();
    }

    @Test
    void deleteImage_returnsFalseWhenImageDoesNotBelongToPlant() throws IOException {
        when(plantImageRepository.existsByIdAndPlantId(5L, 1L)).thenReturn(false);

        boolean result = plantService.deleteImage(1L, 5L);

        assertThat(result).isFalse();
        verify(plantImageRepository, never()).deleteById(any());
    }
}
