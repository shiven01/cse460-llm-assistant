package com.cse460.llm_assistant.service;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentImage;
import com.cse460.llm_assistant.repository.DocumentImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageExtractionService {

    private final DocumentImageRepository imageRepository;

    /**
     * Extract and store images from a PDF document
     *
     * @param document The document entity
     * @param pdDocument The PDF document loaded with PDFBox
     * @throws IOException If there's an error processing the document
     */
    public void extractAndStoreImages(Document document, PDDocument pdDocument) throws IOException {
        log.info("Extracting images from document ID: {}", document.getId());

        int pageCount = pdDocument.getNumberOfPages();
        AtomicInteger totalImages = new AtomicInteger(0);

        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int pageNumber = pageIndex + 1; // Convert to 1-based page numbering
            PDPage page = pdDocument.getPage(pageIndex);

            try {
                extractImagesFromPage(document, page, pageNumber, totalImages);
            } catch (Exception e) {
                log.error("Error extracting images from page {}: {}", pageNumber, e.getMessage());
                // Continue with next page even if this one fails
            }
        }

        log.info("Total images extracted from document: {}", totalImages.get());
    }

    private void extractImagesFromPage(Document document, PDPage page, int pageNumber, AtomicInteger totalImages)
            throws IOException {
        PDResources resources = page.getResources();
        if (resources == null) {
            log.info("No resources found on page {}", pageNumber);
            return;
        }

        int imageIndex = 0;
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);

            if (xObject instanceof PDImageXObject) {
                try {
                    PDImageXObject image = (PDImageXObject) xObject;
                    BufferedImage bufferedImage = image.getImage();

                    // Simple diagram detection
                    boolean isDiagram = detectIfDiagram(bufferedImage);

                    // Convert to byte array
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(bufferedImage, "PNG", baos);
                    byte[] imageData = baos.toByteArray();

                    // Create and save document image
                    DocumentImage documentImage = DocumentImage.builder()
                            .document(document)
                            .pageNumber(pageNumber)
                            .x(0.0f) // We don't have position information in this simpler approach
                            .y(0.0f)
                            .width((float) bufferedImage.getWidth())
                            .height((float) bufferedImage.getHeight())
                            .imageData(imageData)
                            .format("PNG")
                            .isDiagram(isDiagram)
                            .description((isDiagram ? "Diagram" : "Image") + " " + (++imageIndex) +
                                    " on page " + pageNumber)
                            .build();

                    imageRepository.save(documentImage);
                    totalImages.incrementAndGet();

                    log.info("Saved {} {} for page {}",
                            isDiagram ? "diagram" : "image", imageIndex, pageNumber);
                } catch (Exception e) {
                    log.error("Error processing image on page {}: {}", pageNumber, e.getMessage());
                    // Continue with next image even if this one fails
                }
            }
        }
    }

    private boolean detectIfDiagram(BufferedImage image) {
        // Simple heuristic - can be improved with more sophisticated approaches
        int width = image.getWidth();
        int height = image.getHeight();

        // Sample some pixels to check for diagram-like characteristics
        int colorCount = countDistinctColors(image, 100);

        // Diagrams often have fewer distinct colors than photos
        return colorCount < 20;
    }

    private int countDistinctColors(BufferedImage image, int sampleSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Sample points from the image
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (int i = 0; i < sampleSize; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            if (x < width && y < height) { // Ensure coordinates are within bounds
                colors.add(image.getRGB(x, y));
            }
        }

        return colors.size();
    }
}