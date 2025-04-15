package com.cse460.llm_assistant.repository;

import com.cse460.llm_assistant.model.EmbeddingDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmbeddingRepository extends ElasticsearchRepository<EmbeddingDocument, String> {
    List<EmbeddingDocument> findByDocumentId(Long documentId);
    void deleteByDocumentId(Long documentId);
}