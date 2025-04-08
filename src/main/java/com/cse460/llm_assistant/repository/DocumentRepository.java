package com.cse460.llm_assistant.repository;

import com.cse460.llm_assistant.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByContentHash(String contentHash);
    List<Document> findByStatus(String status);
}