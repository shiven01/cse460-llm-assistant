package com.cse460.llm_assistant.repository;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentContentRepository extends JpaRepository<DocumentContent, Long> {
    List<DocumentContent> findByDocumentIdOrderByPageNumberAscChunkSequenceAsc(Long documentId);
    List<DocumentContent> findByDocumentAndPageNumberOrderByChunkSequenceAsc(Document document, Integer pageNumber);
}