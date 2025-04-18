package com.cse460.llm_assistant.service;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentImage;
import com.cse460.llm_assistant.repository.DocumentImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageExtractionTest {

    @Mock
    private DocumentImageRepository imageRepository;

    @InjectMocks
    private ImageStorageService imageStorageService;

    // Using a real extractor since we want to test actual PDF processing
    @Spy
    private MultimodalPdfExtractor pdfExtractor = new MultimodalPdfExtractor();

    @TempDir
    Path tempDir;

    private Document testDocument;
    private byte[] pdfBytes;

    @BeforeEach
    void setup() throws IOException {
        // Set up the image storage location
        Path imageStorage = tempDir.resolve("images");
        Files.createDirectories(imageStorage);

        // Set the imageStorageLocation field using reflection
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
                .title("Test Architecture Document")
                .filename("sample-architecture.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .status("PROCESSED")
                .uploadedAt(LocalDateTime.now())
                .processedAt(LocalDateTime.now())
                .pageCount(3)
                .contentHash("abc123")
                .build();

        // Load the actual PDF file
        ClassPathResource resource = new ClassPathResource("test-documents/sample-architecture.pdf");
        pdfBytes = Files.readAllBytes(resource.getFile().toPath());

        // Set up the mock repository to return an ID when saving
        when(imageRepository.save(any(DocumentImage.class))).thenAnswer(invocation -> {
            DocumentImage image = invocation.getArgument(0);
            // Set the ID field using reflection since we don't have a setter
            try {
                java.lang.reflect.Field idField = DocumentImage.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(image, 1L);
            } catch (Exception e) {
                fail("Failed to set ID: " + e.getMessage());
            }
            return image;
        });
    }

    @Test
    void testImageStorage() {
        // Use a simple test image for storage
        byte[] sampleImage = new byte[] {
                (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
                0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08,
                0x02, 0x00, 0x00, 0x00, (byte)0x90, 0x77, 0x53, (byte)0xDE, 0x00, 0x00, 0x00, 0x0C,
                0x49, 0x44, 0x41, 0x54, 0x08, (byte)0xD7, 0x63, (byte)0xF8, (byte)0xCF, (byte)0xC0, 0x00, 0x00,
                0x03, 0x01, 0x01, 0x00, 0x18, (byte)0xDD, (byte)0x8D, (byte)0xB0, 0x00, 0x00, 0x00, 0x00,
                0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42, 0x60, (byte)0x82
        };

        // Call the service method
        DocumentImage result = imageStorageService.storeImage(testDocument, sampleImage, 1, 0);

        // Verify the image was saved
        assertNotNull(result);
        assertEquals(1L, result.getId());

        // Verify the repository was called
        verify(imageRepository, times(1)).save(any(DocumentImage.class));

        // Verify the file was created in the temp directory
        assertTrue(Files.exists(tempDir.resolve("images").resolve(result.getImagePath())));
    }

    @Test
    void testRealPdfImageExtraction() throws IOException {
        // Test with the actual PDF file
        Map<Integer, List<byte[]>> extractedImages = pdfExtractor.extractImages(pdfBytes);

        // Verify that images were extracted
        assertNotNull(extractedImages);
        assertFalse(extractedImages.isEmpty(), "The PDF should contain at least one image");

        // Print some debug information
        System.out.println("Extracted images from " + extractedImages.size() + " pages");
        extractedImages.forEach((page, images) ->
                System.out.println("Page " + page + ": " + images.size() + " images"));

        // Test the full integration by creating our service and calling processImages
        PdfProcessingService pdfService = new PdfProcessingService(
                null, null, null, pdfExtractor, imageStorageService
        );

        // Use reflection to call the private method
        try {
            java.lang.reflect.Method method = PdfProcessingService.class.getDeclaredMethod(
                    "processImages", Document.class, byte[].class);
            method.setAccessible(true);
            method.invoke(pdfService, testDocument, pdfBytes);

            // Verify images were stored
            ArgumentCaptor<DocumentImage> imageCaptor = ArgumentCaptor.forClass(DocumentImage.class);
            verify(imageRepository, atLeastOnce()).save(imageCaptor.capture());

            List<DocumentImage> savedImages = imageCaptor.getAllValues();
            System.out.println("Saved " + savedImages.size() + " images to the repository");
            assertFalse(savedImages.isEmpty(), "At least one image should be saved");

        } catch (Exception e) {
            fail("Test failed: " + e.getMessage());
        }
    }
}