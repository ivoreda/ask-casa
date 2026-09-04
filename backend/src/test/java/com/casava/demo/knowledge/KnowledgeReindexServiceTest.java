package com.casava.demo.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casava.demo.product.Product;
import com.casava.demo.product.ProductExclusion;
import com.casava.demo.product.ProductExclusionRepository;
import com.casava.demo.product.ProductFaq;
import com.casava.demo.product.ProductFaqRepository;
import com.casava.demo.product.ProductRepository;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

class KnowledgeReindexServiceTest {

  @TempDir Path tempDir;

  @Test
  @SuppressWarnings("unchecked")
  void reindexWipesStoreThenAddsDocuments() throws Exception {
    ProductRepository productRepo = mock(ProductRepository.class);
    ProductExclusionRepository exclusionRepo = mock(ProductExclusionRepository.class);
    ProductFaqRepository faqRepo = mock(ProductFaqRepository.class);
    SimpleVectorStore vectorStore = mock(SimpleVectorStore.class);

    Product product = new Product();
    product.setId(UUID.randomUUID());
    product.setSlug("device-protection");
    product.setName("Device Protection");
    product.setTagline("Phones and laptops covered");
    product.setMonthlyFrom(new BigDecimal("2500"));
    product.setCoverHighlights(List.of("No deductibles"));
    product.setDescription("Cover for theft, drop, and water damage.");

    ProductExclusion exclusion = new ProductExclusion();
    exclusion.setId(UUID.randomUUID());
    exclusion.setProduct(product);
    exclusion.setText("Pre-existing damage is not covered.");

    ProductFaq faq = new ProductFaq();
    faq.setId(UUID.randomUUID());
    faq.setProduct(product);
    faq.setQuestion("How fast is replacement?");
    faq.setAnswer("Typically within 24–72 hours after approval.");

    when(productRepo.findAll()).thenReturn(List.of(product));
    when(exclusionRepo.findAll()).thenReturn(List.of(exclusion));
    when(faqRepo.findAll()).thenReturn(List.of(faq));

    Path storePath = tempDir.resolve("vector-store.json");
    Files.writeString(storePath, "{\"stale-uuid-id\":{}}");

    KnowledgeReindexService service =
        new KnowledgeReindexService(
            productRepo,
            exclusionRepo,
            faqRepo,
            new KnowledgeChunkBuilder(),
            vectorStore,
            storePath);

    service.reindex();

    // Must clear ALL prior vectors (not only new UUID-based IDs), else H2 reseed leaves orphans.
    verify(vectorStore).delete(any(Filter.Expression.class));
    ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
    verify(vectorStore).add(captor.capture());
    verify(vectorStore).save(any(File.class));

    assertThat(captor.getValue()).isNotEmpty();
    assertThat(captor.getValue().getFirst().getMetadata())
        .containsKeys("sourceType", "productSlug", "title", "sourceId", "productName");
  }
}
