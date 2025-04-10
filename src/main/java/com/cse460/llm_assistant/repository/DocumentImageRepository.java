package com.cse460.llm_assistant.repository;

import com.cse460.llm_assistant.model.Document;
import com.cse460.llm_assistant.model.DocumentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentImageRepository extends JpaRepository<DocumentImage, Long> {
    List<DocumentImage> findByDocumentIdOrderByPageNumberAsc(Long documentId);
    List<DocumentImage> findByDocumentAndPageNumber(Document document, Integer pageNumber);
    List<DocumentImage> findByDocumentIdAndPageNumber(Long documentId, Integer pageNumber);
}