package com.cse460.llm_assistant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "document_contents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    private Integer pageNumber;

    // Section within the page (optional)
    private String section;

    // The actual text content
    @Column(columnDefinition = "TEXT")
    private String content;

    // Text chunk sequence number for long texts split into chunks
    private Integer chunkSequence;
}