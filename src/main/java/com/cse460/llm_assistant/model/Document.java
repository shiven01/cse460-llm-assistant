package com.cse460.llm_assistant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String filename;
    private String contentType;
    private Long fileSize;
    private String status; // UPLOADED, PROCESSING, PROCESSED, FAILED

    @Column(length = 1000)
    private String description;

    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;

    // Number of pages in the document
    private Integer pageCount;

    // Hash of the file content to avoid duplicates
    private String contentHash;
}