package com.cse460.llm_assistant.service;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentImage;
import com.cse460.llm_assistant.repository.DocumentImageRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
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
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
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
        // Set up the image storage location to the specific path
        Path imageStorage = Paths.get("/Users/shivenshekar/Desktop/pdf-images");

        // Create directories if they don't exist
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

        lenient().when(imageRepository.save(any(DocumentImage.class))).thenAnswer(invocation -> {
            DocumentImage image = invocation.getArgument(0);
            // Set ID using reflection
            Field idField = DocumentImage.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(image, 1L);
            return image;
        });
    }

    @Test
    void testImageStorage() throws IOException {
        // Create a more realistic test image instead of using the minimal PNG bytes
        // This will create a simple 100x100 colored rectangle image
        BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);

        // Fill with a red color (similar to what you showed in your screenshot)
        Graphics2D g2d = testImage.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, 100, 100);
        g2d.dispose();

        // Convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "PNG", baos);
        byte[] sampleImage = baos.toByteArray();

        // Log the sample image size to verify it's large enough
        System.out.println("Created test image with size: " + sampleImage.length + " bytes");

        // Save test image to temp directory for visual inspection
        String testImagePath = tempDir.resolve("test_red_square.png").toString();
        ImageIO.write(testImage, "PNG", new File(testImagePath));
        System.out.println("Test image saved to: " + testImagePath);

        // Call the service method
        DocumentImage result = imageStorageService.storeImage(testDocument, sampleImage, 1, 0);

        // Verify the image was saved
        assertNotNull(result, "Image should be stored successfully");
        assertEquals(1L, result.getId());

        // Verify the repository was called
        verify(imageRepository, times(1)).save(any(DocumentImage.class));

        // Verify the file was created in the specified directory
        Path savedImagePath = Paths.get("/Users/shivenshekar/Desktop/pdf-images").resolve(result.getImagePath());
        assertTrue(Files.exists(savedImagePath));
        System.out.println("Stored image at: " + savedImagePath);
    }

    /**
     * Test direct PDF rendering without using the extractor
     * This helps isolate whether the issue is with PDFBox or with our extractor code
     */
    @Test
    void testDirectPdfRendering() throws IOException {
        // Load the PDF directly using PDFBox
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);

            // Disable subsampling for better quality
            renderer.setSubsamplingAllowed(false);

            System.out.println("Direct test - PDF has " + document.getNumberOfPages() + " pages");

            // Render the first page as a test
            BufferedImage renderedPage = renderer.renderImageWithDPI(0, 300, ImageType.RGB);

            // Verify image properties
            assertNotNull(renderedPage, "Rendered image should not be null");
            System.out.println("Direct rendered dimensions: " + renderedPage.getWidth() + "x" + renderedPage.getHeight());
            assertTrue(renderedPage.getWidth() > 100 && renderedPage.getHeight() > 100,
                    "Image should have meaningful dimensions");

            // Save to file for visual inspection
            String directTestPath = tempDir.resolve("direct_test_page.png").toString();
            ImageIO.write(renderedPage, "PNG", new File(directTestPath));
            System.out.println("Direct test image saved to: " + directTestPath);

            // Save to byte array to verify size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(renderedPage, "PNG", baos);
            byte[] imageData = baos.toByteArray();

            System.out.println("Direct test image size: " + imageData.length + " bytes");
            assertTrue(imageData.length > 5000, "Image data should be substantial");
        }
    }

    @Test
    void testPdfPageRendering() throws IOException {
        // Test with the actual PDF file - should now render all pages as images
        Map<Integer, List<byte[]>> renderedPages = pdfExtractor.extractImages(pdfBytes);

        // Verify that pages were rendered
        assertNotNull(renderedPages);
        assertFalse(renderedPages.isEmpty(), "The PDF should have rendered at least one page");

        // Check that we have the expected number of pages
        int expectedPageCount = 0;
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            expectedPageCount = doc.getNumberOfPages();
            System.out.println("PDF document has " + expectedPageCount + " pages");
        }

        assertEquals(expectedPageCount, renderedPages.size(),
                "Should have rendered all pages in the document");

        // Print some debug information
        System.out.println("Rendered " + renderedPages.size() + " pages as images");

        // Detailed verification of each page rendering
        renderedPages.forEach((page, images) -> {
            System.out.println("Page " + page + ": " + images.size() + " images");

            // There should be exactly one image per page
            assertEquals(1, images.size(), "Each page should have exactly one rendered image");

            // Verify the image has substantial data (typical PDF page should be at least 10KB)
            byte[] imageData = images.get(0);
            System.out.println("  - Image size: " + imageData.length + " bytes");
            assertTrue(imageData.length > 5000,
                    "Rendered image should have substantial data (>5KB); got " + imageData.length + " bytes");

            // Try to read the image data to verify it's valid
            try {
                BufferedImage testImage = ImageIO.read(new ByteArrayInputStream(imageData));
                assertNotNull(testImage, "Image data should be readable as an image");
                System.out.println("  - Valid image dimensions: " + testImage.getWidth() + "x" + testImage.getHeight());
                assertTrue(testImage.getWidth() > 50 && testImage.getHeight() > 50,
                        "Image should have reasonable dimensions; got " +
                                testImage.getWidth() + "x" + testImage.getHeight());

                // Write the image to a file in the temp directory for visual inspection
                String testFilePath = tempDir.resolve("test_page_" + page + ".png").toString();
                ImageIO.write(testImage, "PNG", new File(testFilePath));
                System.out.println("  - Test image written to: " + testFilePath);

            } catch (IOException e) {
                fail("Failed to read image data as a valid image: " + e.getMessage());
            }
        });
    }
}