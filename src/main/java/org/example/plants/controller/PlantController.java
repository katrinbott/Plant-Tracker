package org.example.plants.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.validation.Valid;
import org.example.plants.dto.PlantCreateRequest;
import org.example.plants.model.Plant;
import org.example.plants.service.PlantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/plants")
public class PlantController {

    private final PlantService plantService;
    @Value("${app.base-url}")
    private String baseUrl;

    public PlantController(PlantService plantService) {
        this.plantService = plantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Plant createPlant(@RequestBody @Valid PlantCreateRequest request) {
        return plantService.createPlant(request);
    }

    @GetMapping
    public List<Plant> getAllPlants() {
        return plantService.getAllPlants();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlant(@PathVariable Long id) {
        plantService.deletePlant(id);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getPlantQrCode(@PathVariable Long id)
    throws WriterException, IOException {
        String url = baseUrl + "/water.html?plantId=" + id;

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);

        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(baos.toByteArray());
        }
}