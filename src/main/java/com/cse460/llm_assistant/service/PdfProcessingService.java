package com.cse460.llm_assistant.service;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentContent;
import com.cse460.llm_assistant.repository.DocumentContentRepository;
import com.cse460.llm_assistant.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfProcessingService {

    private final DocumentRepository documentRepository;
    private final DocumentContentRepository contentRepository;
    private final EmbeddingService embeddingService;

    // Maximum content length per chunk
    private static final int MAX_CHUNK_SIZE = 1000;

    public Document processAndStorePdf(MultipartFile file, String title, String description) throws IOException {
        // Log the start of processing
        log.info("Starting to process file: {}, size: {}, content type: {}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // Compute hash to check for duplicates
        String contentHash = computeHash(file);
        log.info("Computed hash: {}", contentHash);

        // Check if the file already exists
        Optional<Document> existingDoc = documentRepository.findByContentHash(contentHash);
        if (existingDoc.isPresent()) {
            log.info("Document already exists with ID: {}", existingDoc.get().getId());
            return existingDoc.get();
        }

        // Create new document
        Document document = Document.builder()
                .title(title == null ? file.getOriginalFilename() : title)
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .status("PROCESSING")
                .description(description)
                .uploadedAt(LocalDateTime.now())
                .contentHash(contentHash)
                .build();

        log.info("Saving document metadata");
        document = documentRepository.save(document);
        log.info("Document saved with ID: {}", document.getId());

        try {
            // Process based on content type
            if (file.getContentType() != null && file.getContentType().toLowerCase().contains("pdf")) {
                log.info("Processing PDF file");
                processPdfFile(document, file);
            }
            // For testing purposes
            else {
                log.info("Processing text file");
                processTextFile(document, file);
            }

            document.setStatus("PROCESSED");
            document.setProcessedAt(LocalDateTime.now());
            log.info("Document processed successfully");

            // Save document before generating embeddings
            document = documentRepository.save(document);

            // Generate embeddings
            embeddingService.processDocumentEmbeddings(document);

        } catch (Exception e) {
            log.error("Error processing file", e);
            document.setStatus("FAILED");
        }

        log.info("Updating document status to {}", document.getStatus());
        return documentRepository.save(document);
    }

    private void processPdfFile(Document document, MultipartFile file) throws IOException {
        log.debug("Starting PDF processing using PDFBox 3.0.4");

        try (PDDocument pdDocument = Loader.loadPDF(file.getBytes())) {
            int pageCount = pdDocument.getNumberOfPages();
            document.setPageCount(pageCount);
            log.info("PDF loaded successfully with {} pages", pageCount);

            // Extract text from the entire document at once
            PDFTextStripper stripper = new PDFTextStripper();
            String allText = stripper.getText(pdDocument);
            log.info("Extracted {} characters of text from the entire document", allText.length());

            // Also extract text page by page for better organization
            for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(pdDocument);
                log.debug("Page {}: extracted {} characters", pageNum, pageText.length());

                // Store text in chunks
                storeTextChunks(document, pageNum, pageText);
            }
        } catch (Exception e) {
            log.error("Error processing PDF: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void processTextFile(Document document, MultipartFile file) throws IOException {
        log.debug("Starting text file processing");
        String text = readFromInputStream(file.getInputStream());
        log.info("Read {} characters from text file", text.length());

        // Store as a single page
        document.setPageCount(1);
        storeTextChunks(document, 1, text);
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }

    private void storeTextChunks(Document document, int pageNum, String pageText) {
        // Simple chunking by size
        List<String> chunks = splitTextIntoChunks(pageText);
        log.info("Split text into {} chunks for page {}", chunks.size(), pageNum);

        for (int i = 0; i < chunks.size(); i++) {
            DocumentContent content = DocumentContent.builder()
                    .document(document)
                    .pageNumber(pageNum)
                    .chunkSequence(i)
                    .content(chunks.get(i))
                    .build();

            contentRepository.save(content);
            log.debug("Saved chunk {} for page {}", i, pageNum);
        }
    }

    private List<String> splitTextIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        // Simple split by character count
        for (int i = 0; i < text.length(); i += PdfProcessingService.MAX_CHUNK_SIZE) {
            chunks.add(text.substring(i, Math.min(i + PdfProcessingService.MAX_CHUNK_SIZE, text.length())));
        }

        return chunks;
    }

    private String computeHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Error computing hash", e);
            return "";
        }
    }
}