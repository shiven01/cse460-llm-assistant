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
            // Generate content hash for deduplication
            String contentHash = generateHash(imageData);

            // Determine image format (default to PNG if can't detect)
            String format = detectImageFormat(imageData).orElse("png");

            // Generate unique filename
            String filename = String.format("%s_p%d_%d_%s.%s",
                    document.getId(),
                    pageNumber,
                    imageSequence,
                    UUID.randomUUID().toString().substring(0, 8),
                    format);

            // Save to filesystem
            Path targetPath = imageStorageLocation.resolve(filename);

            // Convert byte array to BufferedImage for saving
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (bufferedImage == null) {
                log.error("Failed to convert image data to BufferedImage for document: {}, page: {}",
                        document.getId(), pageNumber);
                return null;
            }

            // Save image file
            ImageIO.write(bufferedImage, format, targetPath.toFile());
            log.info("Saved image to: {}", targetPath);

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