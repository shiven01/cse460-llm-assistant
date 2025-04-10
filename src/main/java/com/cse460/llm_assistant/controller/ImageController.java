package com.cse460.llm_assistant.controller;

import com.cse460.llm_assistant.model.DocumentImage;
import com.cse460.llm_assistant.repository.DocumentImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final DocumentImageRepository imageRepository;

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<DocumentImage>> getImagesByDocument(@PathVariable Long documentId) {
        List<DocumentImage> images = imageRepository.findByDocumentIdOrderByPageNumberAsc(documentId);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/document/{documentId}/page/{pageNumber}")
    public ResponseEntity<List<DocumentImage>> getImagesByDocumentAndPage(
            @PathVariable Long documentId,
            @PathVariable Integer pageNumber) {
        List<DocumentImage> images = imageRepository.findByDocumentIdAndPageNumber(documentId, pageNumber);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/document/{documentId}/diagrams")
    public ResponseEntity<List<DocumentImage>> getDiagramsByDocument(@PathVariable Long documentId) {
        List<DocumentImage> images = imageRepository.findByDocumentIdOrderByPageNumberAsc(documentId);
        List<DocumentImage> diagrams = images.stream()
                .filter(img -> img.getIsDiagram() != null && img.getIsDiagram())
                .toList();
        return ResponseEntity.ok(diagrams);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentImage> getImage(@PathVariable Long id) {
        Optional<DocumentImage> image = imageRepository.findById(id);
        return image.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/data")
    public ResponseEntity<byte[]> getImageData(@PathVariable Long id) {
        Optional<DocumentImage> imageOpt = imageRepository.findById(id);
        if (imageOpt.isPresent()) {
            DocumentImage image = imageOpt.get();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("image/" + image.getFormat().toLowerCase()))
                    .body(image.getImageData());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}