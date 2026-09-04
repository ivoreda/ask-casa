package com.casava.demo.knowledge;

import com.casava.demo.product.ProductExclusionRepository;
import com.casava.demo.product.ProductFaqRepository;
import com.casava.demo.product.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeReindexService {

  private static final Logger log = LoggerFactory.getLogger(KnowledgeReindexService.class);

  private final ProductRepository productRepository;
  private final ProductExclusionRepository exclusionRepository;
  private final ProductFaqRepository faqRepository;
  private final KnowledgeChunkBuilder chunkBuilder;
  private final SimpleVectorStore vectorStore;
  private final Path vectorStorePath;

  public KnowledgeReindexService(
      ProductRepository productRepository,
      ProductExclusionRepository exclusionRepository,
      ProductFaqRepository faqRepository,
      KnowledgeChunkBuilder chunkBuilder,
      SimpleVectorStore vectorStore,
      @Value("${casava.ai.vector-store-path:./data/vector-store.json}") Path vectorStorePath) {
    this.productRepository = productRepository;
    this.exclusionRepository = exclusionRepository;
    this.faqRepository = faqRepository;
    this.chunkBuilder = chunkBuilder;
    this.vectorStore = vectorStore;
    this.vectorStorePath = vectorStorePath;
  }

  public void reindex() {
    List<KnowledgeChunkDocument> chunks =
        chunkBuilder.build(
            productRepository.findAll(),
            exclusionRepository.findAll(),
            faqRepository.findAll());

    List<Document> documents = chunks.stream().map(this::toDocument).toList();

    // Clear strategy for SimpleVectorStore demo: use stable document IDs
    // (sourceType:sourceId), delete those IDs, then add. Missing delete-all is fine
    // because put overwrites same IDs and the demo catalog is static.
    List<String> ids = documents.stream().map(Document::getId).toList();
    if (!ids.isEmpty()) {
      vectorStore.delete(ids);
    }

    if (!documents.isEmpty()) {
      vectorStore.add(documents);
    }

    Path parent = vectorStorePath.getParent();
    if (parent != null) {
      try {
        Files.createDirectories(parent);
      } catch (java.io.IOException e) {
        throw new IllegalStateException("Failed to create vector store directory: " + parent, e);
      }
    }
    vectorStore.save(vectorStorePath.toFile());

    log.info("Indexed {} knowledge chunks into SimpleVectorStore", documents.size());
  }

  public boolean isStoreEmpty() {
    try {
      return !Files.exists(vectorStorePath) || Files.size(vectorStorePath) == 0;
    } catch (java.io.IOException e) {
      return true;
    }
  }

  private Document toDocument(KnowledgeChunkDocument chunk) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("title", chunk.title());
    metadata.put("sourceType", chunk.sourceType());
    metadata.put("sourceId", chunk.sourceId());
    metadata.put("productSlug", chunk.productSlug());
    metadata.put("productName", chunk.productName());
    return new Document(chunk.sourceType() + ":" + chunk.sourceId(), chunk.content(), metadata);
  }
}
