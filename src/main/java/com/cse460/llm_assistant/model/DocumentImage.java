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

    // Image position on page
    private Float x;
    private Float y;
    private Float width;
    private Float height;

    // Image data stored as a binary blob
    @Lob
    @Column(columnDefinition = "BYTEA")
    private byte[] imageData;

    // Image format (png, jpg, etc.)
    private String format;

    // Is this a diagram/chart?
    private Boolean isDiagram;

    // Optional description or extracted caption
    @Column(length = 1000)
    private String description;
}