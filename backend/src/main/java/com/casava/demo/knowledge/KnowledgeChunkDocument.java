package com.casava.demo.knowledge;

public record KnowledgeChunkDocument(
    String content,
    String title,
    String sourceType,
    String sourceId,
    String productSlug,
    String productName
) {}
