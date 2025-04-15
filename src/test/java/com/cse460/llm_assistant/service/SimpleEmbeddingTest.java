package com.cse460.llm_assistant.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.cse460.llm_assistant.repository.EmbeddingRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class SimpleEmbeddingTest {

    @InjectMocks
    private EmbeddingService embeddingService;

    @Mock
    private EmbeddingRepository embeddingRepository;

    @Test
    public void testEmbeddingGeneration() {
        // Test simple embedding generation
        String testText = "This is a test text for embedding generation";
        List<Float> embedding = embeddingService.generateEmbedding(testText);

        // Assertions
        assertNotNull(embedding);
        assertTrue(embedding.size() > 0);
        System.out.println("Generated embedding with " + embedding.size() + " dimensions");
        System.out.println("Sample embedding values: " + embedding.subList(0, 5));
    }
}