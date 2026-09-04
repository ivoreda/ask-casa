package com.casava.demo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

  @Bean
  SimpleVectorStore vectorStore(EmbeddingModel embeddingModel, AiProperties aiProperties)
      throws IOException {
    Path path = Path.of(aiProperties.getVectorStorePath());
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
