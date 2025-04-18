package com.cse460.llm_assistant.service;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentImage;
import com.cse460.llm_assistant.repository.DocumentImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageStorageService {

    private final DocumentImageRepository imageRepository;
    private final Path imageStorageLocation;

    /**
     * Stores an image from a byte array and creates a database entry
     */
    public DocumentImage storeImage(Document document, byte[] imageData, int pageNumber, int imageSequence) {
        try {
            if (imageData == null || imageData.length < 100) {
                log.error("Invalid image data for document: {}, page: {}, size: {}",
                        document.getId(), pageNumber, (imageData != null) ? imageData.length : 0);
                return null;
            }

            // Generate content hash for deduplication
            String contentHash = generateHash(imageData);
            log.debug("Generated content hash: {}", contentHash);

            // Determine image format - verify if the image data is valid
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (bufferedImage == null) {
                log.error("Failed to read image data for document: {}, page: {}, data size: {}",
                        document.getId(), pageNumber, imageData.length);

                // Try to diagnose the image data
                log.debug("First 20 bytes of image data: {}",
                        Arrays.toString(Arrays.copyOf(imageData, Math.min(20, imageData.length))));
                return null;
            }

            // Log image details
            log.info("Successfully read image: dimensions {}x{}, type: {}",
                    bufferedImage.getWidth(), bufferedImage.getHeight(),
                    bufferedImage.getType());

            // Default to PNG format
            String format = "png";

            // Generate unique filename
            String filename = String.format("%s_p%d_%d_%s.%s",
                    document.getId(),
                    pageNumber,
                    imageSequence,
                    UUID.randomUUID().toString().substring(0, 8),
                    format);

            // Save to filesystem
            Path targetPath = imageStorageLocation.resolve(filename);

            // Save image file with high quality settings
            log.info("Saving image to: {}", targetPath);
            boolean success = ImageIO.write(bufferedImage, format, targetPath.toFile());

            if (!success) {
                log.error("Failed to write image to file: no appropriate writer found for format: {}", format);
                return null;
            }

            // Verify file was created and has content
            File savedFile = targetPath.toFile();
            if (!savedFile.exists() || savedFile.length() < 100) {
                log.error("File was not created properly: exists={}, size={}",
                        savedFile.exists(), savedFile.exists() ? savedFile.length() : 0);
                return null;
            }

            log.info("Successfully saved image to {} ({} bytes)", targetPath, savedFile.length());

            // Create and save database entry
            DocumentImage documentImage = DocumentImage.builder()
                    .document(document)
                    .pageNumber(pageNumber)
                    .imageSequence(imageSequence)
                    .imagePath(filename)
                    .format(format)
                    .build();

            return imageRepository.save(documentImage);

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to store image for document: {}, page: {}",
                    document.getId(), pageNumber, e);
            return null;
        }
    }

    /**
     * Gets the file for a stored image
     */
    public File getImageFile(String imagePath) {
        return imageStorageLocation.resolve(imagePath).toFile();
    }

    /**
     * Generate SHA-256 hash of image data for deduplication
     */
    private String generateHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);

        // Convert to hex string
        try (Formatter formatter = new Formatter()) {
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    /**
     * Detect image format from byte array
     */
    private java.util.Optional<String> detectImageFormat(byte[] imageData) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (bufferedImage != null) {
                // We can read it, but need to determine format
                // For simplicity, default to PNG
                return java.util.Optional.of("png");
            }
            return java.util.Optional.empty();
        } catch (IOException e) {
            log.error("Failed to detect image format", e);
            return java.util.Optional.empty();
        }
    }
}