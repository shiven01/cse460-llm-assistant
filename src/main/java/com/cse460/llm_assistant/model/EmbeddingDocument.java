package com.cse460.llm_assistant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "documents")
@Setting(settingPath = "static/es-settings.json")
public class EmbeddingDocument {

    @Id
    private String id;

    private Long documentId;

    private Integer pageNumber;

    private Integer chunkSequence;

    @Field(type = FieldType.Text, analyzer = "english")
    private String content;

    @Field(type = FieldType.Dense_Vector, dims = 384)
    private List<Float> embedding;

    private String metadata;
}