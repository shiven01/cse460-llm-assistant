package com.cse460.llm_assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {

    @Value("${storage.images.location:./uploads/images}")
    private String imagesLocation;

    @Bean
    public Path imageStorageLocation() throws IOException {
        Path location = Paths.get(imagesLocation);
        if (!Files.exists(location)) {
            Files.createDirectories(location);
        }
        return location;
    }
}