package com.cse460.llm_assistant.repository;

import com.cse460.llm_assistant.model.DocumentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentImageRepository extends JpaRepository<DocumentImage, Long> {
    List<DocumentImage> findByDocumentIdOrderByPageNumberAscImageSequenceAsc(Long documentId);
    List<DocumentImage> findByDocumentIdAndPageNumber(Long documentId, Integer pageNumber);
}