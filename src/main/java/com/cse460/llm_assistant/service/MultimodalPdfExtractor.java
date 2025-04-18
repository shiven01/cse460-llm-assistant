package com.cse460.llm_assistant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class MultimodalPdfExtractor {

    /**
     * Extracts text content from a PDF file with page numbers
     */
    public Map<Integer, String> extractText(byte[] pdfData) throws IOException {
        Map<Integer, String> pageTextMap = new HashMap<>();

        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(document);
                pageTextMap.put(i + 1, pageText);
            }
        }

        return pageTextMap;
    }

    /**
     * Renders each page of a PDF as an image
     * Returns a map of page number to list of image byte arrays
     * Each page will have exactly one image in the list (the rendered page)
     */
    public Map<Integer, List<byte[]>> extractImages(byte[] pdfData) throws IOException {
        Map<Integer, List<byte[]>> pageImagesMap = new HashMap<>();

        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFRenderer renderer = new PDFRenderer(document);

            // Disable subsampling for better rendering quality
            renderer.setSubsamplingAllowed(false);

            log.info("Processing PDF with {} pages", document.getNumberOfPages());

            // Process each page in the document
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                int pageNum = i + 1;
                List<byte[]> pageImages = new ArrayList<>();

                try {
                    // Render the page at 300 DPI for good quality
                    BufferedImage renderedPage = renderer.renderImageWithDPI(i, 300, ImageType.RGB);

                    // Verify that the rendered image has content
                    if (renderedPage.getWidth() <= 1 || renderedPage.getHeight() <= 1) {
                        log.warn("Page {} rendered with invalid dimensions: {}x{}",
                                pageNum, renderedPage.getWidth(), renderedPage.getHeight());
                        continue;
                    }

                    // Log image dimensions for debugging
                    log.info("Rendered page {} with dimensions: {}x{}",
                            pageNum, renderedPage.getWidth(), renderedPage.getHeight());

                    // Convert the rendered image to PNG format with high quality settings
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(32768); // Start with larger buffer

                    // Use ImageWriter with optimal compression settings
                    javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
                    javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();

                    // Set up the output
                    javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                    writer.setOutput(ios);

                    // Write the image
                    writer.write(null, new javax.imageio.IIOImage(renderedPage, null, null), param);

                    // Cleanup
                    ios.flush();
                    writer.dispose();
                    ios.close();

                    // Get the bytes and verify size
                    byte[] imageData = baos.toByteArray();

                    // Validate image data
                    if (imageData.length < 100) {
                        log.warn("Page {} rendered with suspiciously small size: {} bytes",
                                pageNum, imageData.length);

                        // Try a simpler approach as fallback
                        baos = new ByteArrayOutputStream(32768);
                        ImageIO.write(renderedPage, "PNG", baos);
                        imageData = baos.toByteArray();

                        log.info("Fallback rendering resulted in {} bytes", imageData.length);
                    }

                    // Log the size of the image data
                    log.info("Page {} image size: {} bytes", pageNum, imageData.length);

                    // Add the image to the list for this page
                    pageImages.add(imageData);

                    // Add the list to the map for this page number
                    pageImagesMap.put(pageNum, pageImages);

                    log.info("Successfully rendered page {} as image", pageNum);
                } catch (Exception e) {
                    log.error("Error rendering page {}: {}", pageNum, e.getMessage(), e);
                }
            }
        }

        return pageImagesMap;
    }
}