package com.cse460.llm_assistant.service;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentContent;
import com.cse460.llm_assistant.model.EmbeddingDocument;
import com.cse460.llm_assistant.repository.DocumentContentRepository;
import com.cse460.llm_assistant.repository.EmbeddingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final DocumentContentRepository contentRepository;
    private final EmbeddingRepository embeddingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Create embedding model
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    public void processDocumentEmbeddings(Document document) {
        log.info("Processing embeddings for document: {}", document.getId());

        // First, delete any existing embeddings for this document
        embeddingRepository.deleteByDocumentId(document.getId());

        // Get all content chunks for the document
        List<DocumentContent> contents = contentRepository.findByDocumentIdOrderByPageNumberAscChunkSequenceAsc(document.getId());

        // Process each content chunk
        for (DocumentContent content : contents) {
            try {
                // Generate embedding for the content
                Embedding embedding = embeddingModel.embed(content.getContent()).content();

                // Create metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("filename", document.getFilename());
                metadata.put("title", document.getTitle());
                metadata.put("page", content.getPageNumber());
                metadata.put("chunk", content.getChunkSequence());

                // Create embedding document
                EmbeddingDocument embeddingDoc = EmbeddingDocument.builder()
                        .documentId(document.getId())
                        .pageNumber(content.getPageNumber())
                        .chunkSequence(content.getChunkSequence())
                        .content(content.getContent())
                        .embedding(embedding.vectorAsList())
                        .metadata(objectMapper.writeValueAsString(metadata))
                        .build();

                // Save to Elasticsearch
                embeddingRepository.save(embeddingDoc);

                log.debug("Saved embedding for document: {}, page: {}, chunk: {}",
                        document.getId(), content.getPageNumber(), content.getChunkSequence());

            } catch (Exception e) {
                log.error("Error processing embedding for document: {}, page: {}, chunk: {}",
                        document.getId(), content.getPageNumber(), content.getChunkSequence(), e);
            }
        }

        log.info("Completed processing embeddings for document: {}", document.getId());
    }

    public List<Float> generateEmbedding(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        return embedding.vectorAsList();
    }

    public List<EmbeddingDocument> findSimilarDocuments(String query, int limit) {
        List<Float> queryEmbedding = generateEmbedding(query);

        // This is where we would normally perform vector search, but since we're
        // using the repository pattern, we'll need to implement custom query methods
        // This is simplified for now - in a real implementation you'd use the Elasticsearch client directly

        // For now, returning an empty list
        return List.of();
    }
}