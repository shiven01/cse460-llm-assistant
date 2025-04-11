package com.cse460.llm_assistant.controller;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentContent;
import com.cse460.llm_assistant.repository.DocumentContentRepository;
import com.cse460.llm_assistant.service.PdfProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final PdfProcessingService pdfProcessingService;
    private final DocumentContentRepository contentRepository;

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

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
        log.info("Retrieving document with ID: {}", id);
        return ResponseEntity.ok().build(); // Placeholder - implement actual logic
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
}