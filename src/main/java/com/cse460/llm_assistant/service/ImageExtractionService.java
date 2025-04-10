package com.cse460.llm_assistant.service;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentImage;
import com.cse460.llm_assistant.repository.DocumentImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageExtractionService {

    private final DocumentImageRepository imageRepository;

    /**
     * Process a PDF file and extract all images
     */
    public List<DocumentImage> extractImages(Document document, MultipartFile file) {
        List<DocumentImage> savedImages = new ArrayList<>();

        try (PDDocument pdfDocument = Loader.loadPDF(file.getBytes())) {
            log.info("PDF loaded successfully. Pages: {}", pdfDocument.getNumberOfPages());

            for (int i = 0; i < pdfDocument.getNumberOfPages(); i++) {
                int pageNumber = i + 1; // Convert to 1-based page numbers
                log.info("Processing page {} for images", pageNumber);

                PDPage page = pdfDocument.getPage(i);
                List<DocumentImage> pageImages = extractImagesFromPage(document, page, pageNumber);

                if (!pageImages.isEmpty()) {
                    log.info("Found {} images on page {}", pageImages.size(), pageNumber);
                    savedImages.addAll(imageRepository.saveAll(pageImages));
                } else {
                    log.info("No images found on page {}", pageNumber);
                }
            }

            log.info("Image extraction complete. Total images extracted: {}", savedImages.size());
        } catch (IOException e) {
            log.error("Error processing PDF for image extraction: {}", e.getMessage(), e);
        }

        return savedImages;
    }

    /**
     * Extract images from a single PDF page
     */
    private List<DocumentImage> extractImagesFromPage(Document document, PDPage page, int pageNumber) {
        List<DocumentImage> images = new ArrayList<>();
        int imageCounter = 0;

        try {
            PDResources resources = page.getResources();
            if (resources == null) {
                log.warn("No resources found on page {}", pageNumber);
                return images;
            }

            // Process each XObject in the page resources
            for (COSName xObjectName : resources.getXObjectNames()) {
                try {
                    PDXObject xObject = resources.getXObject(xObjectName);
                    log.debug("Processing XObject: {}, Type: {}", xObjectName.getName(),
                            xObject.getClass().getSimpleName());

                    if (xObject instanceof PDImageXObject) {
                        PDImageXObject imageObject = (PDImageXObject) xObject;
                        log.info("Found image on page {}: {}", pageNumber, xObjectName.getName());

                        // Get the image
                        BufferedImage bufferedImage = imageObject.getImage();
                        imageCounter++;

                        // Check if it's a diagram
                        boolean isDiagram = detectIfDiagram(bufferedImage);

                        // Convert BufferedImage to byte array
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "PNG", baos);
                        byte[] imageData = baos.toByteArray();

                        // Create DocumentImage entity
                        DocumentImage documentImage = DocumentImage.builder()
                                .document(document)
                                .pageNumber(pageNumber)
                                .x(0.0f)
                                .y(0.0f)
                                .width((float) bufferedImage.getWidth())
                                .height((float) bufferedImage.getHeight())
                                .imageData(imageData)
                                .format("PNG")
                                .isDiagram(isDiagram)
                                .description((isDiagram ? "Diagram" : "Image") + " " + imageCounter +
                                        " on page " + pageNumber)
                                .build();

                        images.add(documentImage);
                        log.info("Successfully processed {} {} on page {}",
                                isDiagram ? "diagram" : "image", imageCounter, pageNumber);
                    }
                } catch (Exception e) {
                    log.error("Error processing XObject {}: {}", xObjectName.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error extracting images from page {}: {}", pageNumber, e.getMessage());
        }

        return images;
    }

    /**
     * Simple detection of diagrams based on color properties and patterns
     */
    private boolean detectIfDiagram(BufferedImage image) {
        try {
            // Get image dimensions
            int width = image.getWidth();
            int height = image.getHeight();

            // Skip tiny images
            if (width < 50 || height < 50) {
                return false;
            }

            // 1. Check color count (diagrams typically have fewer colors)
            int sampleSize = Math.min(1000, width * height);
            int colorCount = countDistinctColors(image, sampleSize);
            boolean hasFewColors = colorCount < 50;

            // 2. Check for straight line patterns (common in diagrams)
            boolean hasLines = detectLines(image);

            log.debug("Image analysis: size={}x{}, colorCount={}, hasFewColors={}, hasLines={}",
                    width, height, colorCount, hasFewColors, hasLines);

            // Decide if it's a diagram
            return hasFewColors || hasLines;

        } catch (Exception e) {
            log.error("Error in diagram detection: {}", e.getMessage());
            return false;
        }
    }

    private int countDistinctColors(BufferedImage image, int sampleSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        java.util.Set<Integer> colors = new java.util.HashSet<>();

        // Sample in a grid pattern rather than randomly
        int xStep = Math.max(1, width / (int)Math.sqrt(sampleSize));
        int yStep = Math.max(1, height / (int)Math.sqrt(sampleSize));

        for (int x = 0; x < width; x += xStep) {
            for (int y = 0; y < height; y += yStep) {
                colors.add(image.getRGB(x, y));
                if (colors.size() > 100) {
                    // If we already have many colors, it's probably not a diagram
                    return colors.size();
                }
            }
        }

        return colors.size();
    }

    private boolean detectLines(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int horizontalLineCount = 0;
        int verticalLineCount = 0;

        // Sample points to look for horizontal lines
        for (int y = height / 10; y < height * 9 / 10; y += height / 10) {
            int sameColorRun = 1;
            int prevColor = image.getRGB(0, y);

            for (int x = 1; x < width; x++) {
                int color = image.getRGB(x, y);
                if (isSimilarColor(color, prevColor)) {
                    sameColorRun++;
                    if (sameColorRun > width / 4) {
                        horizontalLineCount++;
                        break;
                    }
                } else {
                    sameColorRun = 1;
                    prevColor = color;
                }
            }
        }

        // Sample points to look for vertical lines
        for (int x = width / 10; x < width * 9 / 10; x += width / 10) {
            int sameColorRun = 1;
            int prevColor = image.getRGB(x, 0);

            for (int y = 1; y < height; y++) {
                int color = image.getRGB(x, y);
                if (isSimilarColor(color, prevColor)) {
                    sameColorRun++;
                    if (sameColorRun > height / 4) {
                        verticalLineCount++;
                        break;
                    }
                } else {
                    sameColorRun = 1;
                    prevColor = color;
                }
            }
        }

        return horizontalLineCount >= 2 || verticalLineCount >= 2;
    }

    private boolean isSimilarColor(int color1, int color2) {
        // Extract RGB components
        int r1 = (color1 >> 16) & 0xff;
        int g1 = (color1 >> 8) & 0xff;
        int b1 = color1 & 0xff;

        int r2 = (color2 >> 16) & 0xff;
        int g2 = (color2 >> 8) & 0xff;
        int b2 = color2 & 0xff;

        // Calculate Euclidean distance
        double distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));
        return distance < 30; // Adjust threshold as needed
    }
}