package com.casava.demo.knowledge;

import com.casava.demo.product.Product;
import com.casava.demo.product.ProductExclusion;
import com.casava.demo.product.ProductFaq;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeChunkBuilder {

  public List<KnowledgeChunkDocument> build(
      List<Product> products,
      List<ProductExclusion> exclusions,
      List<ProductFaq> faqs) {
    List<KnowledgeChunkDocument> chunks = new ArrayList<>();

    for (Product product : products) {
      chunks.add(overviewChunk(product));
    }
    for (ProductExclusion exclusion : exclusions) {
      chunks.add(exclusionChunk(exclusion));
    }
    for (ProductFaq faq : faqs) {
      chunks.add(faqChunk(faq));
    }

    return chunks;
  }

  private KnowledgeChunkDocument overviewChunk(Product product) {
    String highlights =
        product.getCoverHighlights() == null
            ? ""
            : product.getCoverHighlights().stream().collect(Collectors.joining(", "));
    String content =
        String.join(
            "\n",
            product.getName(),
            nullToEmpty(product.getTagline()),
            "Monthly from: " + product.getMonthlyFrom(),
            "Highlights: " + highlights,
            nullToEmpty(product.getDescription()));
    return new KnowledgeChunkDocument(
        content,
        product.getName(),
        "product_overview",
        product.getId().toString(),
        product.getSlug(),
        product.getName());
  }

  private KnowledgeChunkDocument exclusionChunk(ProductExclusion exclusion) {
    Product product = exclusion.getProduct();
    return new KnowledgeChunkDocument(
        exclusion.getText(),
        product.getName() + " exclusion",
        "exclusion",
        exclusion.getId().toString(),
        product.getSlug(),
        product.getName());
  }

  private KnowledgeChunkDocument faqChunk(ProductFaq faq) {
    Product product = faq.getProduct();
    String content = "Q: " + faq.getQuestion() + "\nA: " + faq.getAnswer();
    return new KnowledgeChunkDocument(
        content,
        faq.getQuestion(),
        "faq",
        faq.getId().toString(),
        product.getSlug(),
        product.getName());
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
