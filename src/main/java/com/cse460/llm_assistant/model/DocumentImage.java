package com.cse460.llm_assistant.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "document_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    private Integer pageNumber;

    private Integer imageSequence;

    // Path to stored image or content hash
    private String imagePath;

    // Image format (PNG, JPEG, etc.)
    private String format;

    // Optional caption or description
    @Column(length = 1000)
    private String caption;
}