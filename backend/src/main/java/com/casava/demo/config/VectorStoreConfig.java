package com.casava.demo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

  @Bean
  SimpleVectorStore vectorStore(
      EmbeddingModel embeddingModel,
      @Value("${casava.ai.vector-store-path:./data/vector-store.json}") String vectorStorePath)
      throws IOException {
    Path path = Path.of(vectorStorePath);
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
    if (Files.exists(path) && Files.size(path) > 0) {
      store.load(path.toFile());
    }
    return store;
  }
}
