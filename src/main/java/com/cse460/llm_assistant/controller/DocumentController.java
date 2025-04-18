package com.cse460.llm_assistant.controller;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentImage;
import com.cse460.llm_assistant.repository.DocumentImageRepository;
import com.cse460.llm_assistant.repository.DocumentContentRepository;
import com.cse460.llm_assistant.model.DocumentContent;
import com.cse460.llm_assistant.service.PdfProcessingService;
import com.cse460.llm_assistant.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final PdfProcessingService pdfProcessingService;
    private final DocumentContentRepository contentRepository;
    private final DocumentImageRepository imageRepository;
    private final ImageStorageService imageStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {

        log.info("Received upload request for file: {}, size: {}", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            log.error("Empty file received");
            Map<String, String> error = new HashMap<>();
            error.put("error", "File is empty");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Document document = pdfProcessingService.processAndStorePdf(file, title, description);
            log.info("Document processed successfully with ID: {}", document.getId());
            return ResponseEntity.ok(document);
        } catch (IOException e) {
            log.error("Error processing file: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/{id}/text")
    public ResponseEntity<?> getDocumentText(@PathVariable Long id) {
        log.info("Retrieving text for document with ID: {}", id);

        List<DocumentContent> contents = contentRepository.findByDocumentIdOrderByPageNumberAscChunkSequenceAsc(id);

        if (contents.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No content found for document with ID: " + id);
            return ResponseEntity.notFound().build();
        }

        StringBuilder fullText = new StringBuilder();
        int currentPage = 0;

        for (DocumentContent content : contents) {
            if (content.getPageNumber() > currentPage) {
                currentPage = content.getPageNumber();
                fullText.append("\n\n--- PAGE ").append(currentPage).append(" ---\n\n");
            }
            fullText.append(content.getContent());
        }

        Map<String, String> response = new HashMap<>();
        response.put("text", fullText.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all images associated with a document
     */
    @GetMapping("/{id}/images")
    public ResponseEntity<?> getDocumentImages(@PathVariable Long id) {
        log.info("Retrieving images for document with ID: {}", id);

        List<DocumentImage> images = imageRepository.findByDocumentIdOrderByPageNumberAscImageSequenceAsc(id);

        if (images.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No images found for document with ID: " + id);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(images);
    }

    /**
     * Get all images on a specific page of a document
     */
    @GetMapping("/{id}/pages/{pageNumber}/images")
    public ResponseEntity<?> getPageImages(
            @PathVariable Long id,
            @PathVariable Integer pageNumber) {

        log.info("Retrieving images for document with ID: {} page: {}", id, pageNumber);

        List<DocumentImage> images = imageRepository.findByDocumentIdAndPageNumber(id, pageNumber);

        if (images.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No images found for document with ID: " + id + " on page " + pageNumber);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(images);
    }

    /**
     * Get a specific image file
     */
    @GetMapping("/images/{imageId}")
    public ResponseEntity<Resource> getImage(@PathVariable Long imageId) {
        log.info("Retrieving image with ID: {}", imageId);

        Optional<DocumentImage> imageOptional = imageRepository.findById(imageId);

        if (imageOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DocumentImage image = imageOptional.get();
        File imageFile = imageStorageService.getImageFile(image.getImagePath());

        if (!imageFile.exists()) {
            log.error("Image file not found: {}", image.getImagePath());
            return ResponseEntity.notFound().build();
        }

        // Determine media type based on format
        String contentType = switch (image.getFormat().toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "tiff" -> "image/tiff";
            default -> "application/octet-stream";
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + imageFile.getName() + "\"")
                .body(new FileSystemResource(imageFile));
    }
}