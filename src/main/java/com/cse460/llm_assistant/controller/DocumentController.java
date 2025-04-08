// src/main/java/com/cse460/llm_assistant/controller/DocumentController.java
package com.cse460.llm_assistant.controller;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.service.PdfProcessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final PdfProcessingService pdfProcessingService;

    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Document document = pdfProcessingService.processAndStorePdf(file, title, description);
            return ResponseEntity.ok(document);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable Long id) {
        // Implementation to retrieve document by ID
        return ResponseEntity.notFound().build();
    }
}