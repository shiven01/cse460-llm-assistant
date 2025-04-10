// src/main/java/com/cse460/llm_assistant/service/PdfProcessingService.java
package com.cse460.llm_assistant.service;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentContent;
import com.cse460.llm_assistant.model.DocumentImage;
import com.cse460.llm_assistant.repository.DocumentContentRepository;
import com.cse460.llm_assistant.repository.DocumentImageRepository;
import com.cse460.llm_assistant.repository.DocumentRepository;
import com.cse460.llm_assistant.util.DiagramDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
    private final DocumentImageRepository imageRepository;

    // Maximum content length per chunk
    private static final int MAX_CHUNK_SIZE = 1000;

    public Document processAndStorePdf(MultipartFile file, String title, String description) throws IOException {
        // Log the start of processing
        log.info("Starting to process file: {}", file.getOriginalFilename());

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
            if (file.getContentType() != null && file.getContentType().equals("application/pdf")) {
                log.info("Processing PDF file");
                processPdfFile(document, file);
            } else {
                log.info("Processing text file");
                processTextFile(document, file);
            }

            document.setStatus("PROCESSED");
            document.setProcessedAt(LocalDateTime.now());
            log.info("Document processed successfully");

        } catch (Exception e) {
            log.error("Error processing file", e);
            document.setStatus("FAILED");
        }

        log.info("Updating document status to {}", document.getStatus());
        return documentRepository.save(document);
    }

    private void processPdfFile(Document document, MultipartFile file) throws IOException {
        try (PDDocument pdDocument = PDDocument.load(file.getInputStream())) {
            document.setPageCount(pdDocument.getNumberOfPages());

            // Extract text page by page
            PDFTextStripper stripper = new PDFTextStripper();
            for (int pageNum = 1; pageNum <= pdDocument.getNumberOfPages(); pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);
                String pageText = stripper.getText(pdDocument);

                // Store text in chunks
                storeTextChunks(document, pageNum, pageText);

                // Extract images from this page
                extractAndStoreImages(document, pdDocument, pageNum);
            }
        }
    }

    private void extractAndStoreImages(Document document, PDDocument pdDocument, int pageNum) throws IOException {
        PDPage page = pdDocument.getPage(pageNum - 1); // PDFBox uses 0-based indexing
        ImageExtractor imageExtractor = new ImageExtractor();
        imageExtractor.processPage(page);

        List<ExtractedImage> extractedImages = imageExtractor.getImages();
        log.info("Extracted {} images from page {}", extractedImages.size(), pageNum);

        for (int i = 0; i < extractedImages.size(); i++) {
            ExtractedImage extractedImage = extractedImages.get(i);
            BufferedImage bufferedImage = extractedImage.getImage();

            // Check if this image is likely a diagram
            boolean isDiagram = DiagramDetector.isDiagram(bufferedImage);

            // Convert BufferedImage to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", baos);
            byte[] imageData = baos.toByteArray();

            // Create and save image entity
            DocumentImage documentImage = DocumentImage.builder()
                    .document(document)
                    .pageNumber(pageNum)
                    .x(extractedImage.getX())
                    .y(extractedImage.getY())
                    .width((float) bufferedImage.getWidth())
                    .height((float) bufferedImage.getHeight())
                    .imageData(imageData)
                    .format("PNG")
                    .isDiagram(isDiagram)
                    .description(isDiagram ?
                            "Diagram/Chart " + (i + 1) + " on page " + pageNum :
                            "Image " + (i + 1) + " on page " + pageNum)
                    .build();

            imageRepository.save(documentImage);
            log.info("Saved {} {} for page {}", isDiagram ? "diagram" : "image", i, pageNum);
        }
    }

    // Inner class to handle image extraction
    private static class ImageExtractor extends PDFStreamEngine {
        private final List<ExtractedImage> images = new ArrayList<>();
        private float x = 0;
        private float y = 0;

        public List<ExtractedImage> getImages() {
            return images;
        }

        @Override
        protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
            String operation = operator.getName();

            if ("Do".equals(operation)) {
                COSName objectName = (COSName) operands.get(0);
                PDXObject xobject = getResources().getXObject(objectName);

                if (xobject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xobject;
                    BufferedImage bufferedImage = image.getImage();
                    images.add(new ExtractedImage(bufferedImage, x, y));
                } else if (xobject instanceof PDFormXObject) {
                    PDFormXObject form = (PDFormXObject) xobject;
                    showForm(form);
                }
            } else if ("cm".equals(operation)) {
                // cm operator defines a transformation matrix - we could use it to get precise image positioning
                // For simplicity, we're just capturing the current coordinates
                if (operands.size() >= 6) {
                    x = ((COSBase) operands.get(4)).toString().contains(".") ?
                            Float.parseFloat(((COSBase) operands.get(4)).toString()) : 0;
                    y = ((COSBase) operands.get(5)).toString().contains(".") ?
                            Float.parseFloat(((COSBase) operands.get(5)).toString()) : 0;
                }
            }

            super.processOperator(operator, operands);
        }
    }

    // Helper class to store extracted image with position
    private static class ExtractedImage {
        private final BufferedImage image;
        private final float x;
        private final float y;

        public ExtractedImage(BufferedImage image, float x, float y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }

        public BufferedImage getImage() {
            return image;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }

    private void processTextFile(Document document, MultipartFile file) throws IOException {
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
        List<String> chunks = splitTextIntoChunks(pageText, MAX_CHUNK_SIZE);
        log.info("Split text into {} chunks for page {}", chunks.size(), pageNum);

        for (int i = 0; i < chunks.size(); i++) {
            DocumentContent content = DocumentContent.builder()
                    .document(document)
                    .pageNumber(pageNum)
                    .chunkSequence(i)
                    .content(chunks.get(i))
                    .build();

            contentRepository.save(content);
            log.info("Saved chunk {} for page {}", i, pageNum);
        }
    }

    private List<String> splitTextIntoChunks(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();

        // Simple split by character count
        for (int i = 0; i < text.length(); i += maxChunkSize) {
            chunks.add(text.substring(i, Math.min(i + maxChunkSize, text.length())));
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