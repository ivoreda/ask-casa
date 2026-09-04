package com.casava.demo.knowledge;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Reindexes product knowledge into SimpleVectorStore after {@link
 * com.casava.demo.product.ProductSeedRunner}.
 *
 * <p>Runs when {@code casava.knowledge.reindex-on-startup=true} (default) or when the store file is
 * empty.
 */
@Component
@Order(2)
public class KnowledgeReindexRunner implements ApplicationRunner {

  private final KnowledgeReindexService reindexService;
  private final boolean reindexOnStartup;

  public KnowledgeReindexRunner(
      KnowledgeReindexService reindexService,
      @Value("${casava.knowledge.reindex-on-startup:true}") boolean reindexOnStartup) {
    this.reindexService = reindexService;
    this.reindexOnStartup = reindexOnStartup;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (reindexOnStartup || reindexService.isStoreEmpty()) {
      reindexService.reindex();
    }
  }
}
