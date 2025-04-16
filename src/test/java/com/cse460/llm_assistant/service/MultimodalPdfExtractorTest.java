package com.cse460.llm_assistant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MultimodalPdfExtractorTest {

    @InjectMocks
    private MultimodalPdfExtractor extractor;

    @Test
    void testExtractText() throws IOException {
        // Load a test PDF from resources
        ClassPathResource resource = new ClassPathResource("test-documents/sample-architecture.pdf");
        byte[] pdfData = Files.readAllBytes(resource.getFile().toPath());

        // Extract text
        Map<Integer, String> pageTextMap = extractor.extractText(pdfData);

        // Assert
        assertNotNull(pageTextMap);
        assertFalse(pageTextMap.isEmpty());
        assertTrue(pageTextMap.containsKey(1)); // At least page 1 exists
        assertFalse(pageTextMap.get(1).isEmpty()); // Text isn't empty
    }

    @Test
    void testExtractImages() throws IOException {
        // Load a test PDF from resources
        ClassPathResource resource = new ClassPathResource("test-documents/sample-architecture.pdf");
        byte[] pdfData = Files.readAllBytes(resource.getFile().toPath());

        // Extract images
        Map<Integer, List<byte[]>> pageImagesMap = extractor.extractImages(pdfData);

        // Assert
        assertNotNull(pageImagesMap);
        // If the test PDF has images, verify they were extracted
        if (!pageImagesMap.isEmpty()) {
            List<byte[]> images = pageImagesMap.entrySet().iterator().next().getValue();
            assertFalse(images.isEmpty());
            assertTrue(images.get(0).length > 0); // Image has content
        }
    }
}