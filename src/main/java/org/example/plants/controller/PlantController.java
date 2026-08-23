package org.example.plants.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.validation.Valid;
import org.example.plants.dto.PlantCreateRequest;
import org.example.plants.model.Plant;
import org.example.plants.model.PlantImage;
import org.example.plants.service.PlantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.MediaTypeFactory;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public PlantImage uploadImage(@PathVariable Long id,
                                  @RequestParam("file") MultipartFile file,
                                  @RequestParam(required = false) String note) throws IOException {
        return plantService.addImage(id, file, note);
    }

    @GetMapping("/{id}/images")
    public List<PlantImage> getImages(@PathVariable Long id) {
        return plantService.getImages(id);
    }

    @GetMapping("/{id}/images/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long id,
                                           @PathVariable Long imageId) throws IOException {
        byte[] bytes = plantService.getImageBytes(imageId);
        if (bytes == null) return ResponseEntity.notFound().build();
        PlantImage image = plantService.getImages(id).stream()
                .filter(img -> img.getId().equals(imageId)).findFirst().orElse(null);
        MediaType mediaType = image != null
                ? MediaTypeFactory.getMediaType(image.getImagePath()).orElse(MediaType.APPLICATION_OCTET_STREAM)
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(bytes);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id,
                                            @PathVariable Long imageId) throws IOException {
        return plantService.deleteImage(id, imageId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}