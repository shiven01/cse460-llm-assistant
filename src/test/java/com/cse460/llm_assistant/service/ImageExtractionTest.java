package com.cse460.llm_assistant.service;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentImage;
import com.cse460.llm_assistant.repository.DocumentImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageExtractionTest {

    @Mock
    private DocumentImageRepository imageRepository;

    @Spy
    private MultimodalPdfExtractor pdfExtractor;

    @Mock
    private DocumentImage savedImage;

    @TempDir
    Path tempDir;

    @InjectMocks
    private ImageStorageService imageStorageService;

    private Document testDocument;
    private byte[] sampleImage;

    @BeforeEach
    void setup() throws IOException {
        // Set up the image storage location
        Path imageStorage = tempDir.resolve("images");
        Files.createDirectories(imageStorage);

        // Create a reflection hack to set the imageStorageLocation field
        try {
            java.lang.reflect.Field field = ImageStorageService.class.getDeclaredField("imageStorageLocation");
            field.setAccessible(true);
            field.set(imageStorageService, imageStorage);
        } catch (Exception e) {
            fail("Failed to set up test: " + e.getMessage());
        }

        // Create a test document
        testDocument = Document.builder()
                .id(1L)
                .title("Test Document")
                .filename("test.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .status("PROCESSED")
                .uploadedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .pageCount(3)
                .contentHash("abc123")
                .build();

        // Mock a sample image (1x1 pixel black PNG)
        sampleImage = new byte[] {
                (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
                0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08,
                0x02, 0x00, 0x00, 0x00, (byte)0x90, 0x77, 0x53, (byte)0xDE, 0x00, 0x00, 0x00, 0x0C,
                0x49, 0x44, 0x41, 0x54, 0x08, (byte)0xD7, 0x63, (byte)0xF8, (byte)0xCF, (byte)0xC0, 0x00, 0x00,
                0x03, 0x01, 0x01, 0x00, 0x18, (byte)0xDD, (byte)0x8D, (byte)0xB0, 0x00, 0x00, 0x00, 0x00,
                0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42, 0x60, (byte)0x82
        };

        // Mock the saved image response
        when(imageRepository.save(any())).thenReturn(savedImage);
        when(savedImage.getId()).thenReturn(1L);
        when(savedImage.getImagePath()).thenReturn("test_image.png");
    }

    @Test
    void testImageStorage() {
        // Call the service method
        DocumentImage result = imageStorageService.storeImage(testDocument, sampleImage, 1, 0);

        // Verify the image was saved
        assertNotNull(result);
        assertEquals(1L, result.getId());

        // Verify the repository was called
        verify(imageRepository, times(1)).save(any(DocumentImage.class));
    }

    @Test
    void testImageExtraction() throws IOException {
        // Mock the PDF extractor to return our sample image
        Map<Integer, List<byte[]>> mockImages = new HashMap<>();
        mockImages.put(1, List.of(sampleImage));
        when(pdfExtractor.extractImages(any())).thenReturn(mockImages);

        // Create a simple implementation for testing
        PdfProcessingService pdfService = new PdfProcessingService(
                null, null, null, pdfExtractor, imageStorageService
        );

        // Use reflection to call the private method
        try {
            java.lang.reflect.Method method = PdfProcessingService.class.getDeclaredMethod(
                    "processImages", Document.class, byte[].class);
            method.setAccessible(true);
            method.invoke(pdfService, testDocument, new byte[0]); // empty PDF data is fine as we're mocking

            // Verify the image was stored
            verify(imageStorageService, times(1)).storeImage(
                    eq(testDocument), eq(sampleImage), eq(1), eq(0));
        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
}