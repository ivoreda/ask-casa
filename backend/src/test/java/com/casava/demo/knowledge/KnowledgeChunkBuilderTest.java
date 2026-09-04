package com.casava.demo.knowledge;

import com.casava.demo.product.Product;
import com.casava.demo.product.ProductExclusion;
import com.casava.demo.product.ProductFaq;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkBuilderTest {

    @Test
    void buildsOverviewExclusionAndFaqChunks() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSlug("device-protection");
        product.setName("Device Protection");
        product.setTagline("Phones and laptops covered");
        product.setMonthlyFrom(new BigDecimal("2500"));
        product.setCoverHighlights(List.of("No deductibles", "Up to ₦1,500,000"));
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

        KnowledgeChunkBuilder builder = new KnowledgeChunkBuilder();
        List<KnowledgeChunkDocument> chunks = builder.build(List.of(product), List.of(exclusion), List.of(faq));

        assertThat(chunks).hasSize(3);
        assertThat(chunks).anyMatch(c -> "product_overview".equals(c.sourceType()) && c.content().contains("Device Protection"));
        assertThat(chunks).anyMatch(c -> "exclusion".equals(c.sourceType()) && c.content().contains("Pre-existing"));
        assertThat(chunks).anyMatch(c -> "faq".equals(c.sourceType()) && c.content().contains("How fast is replacement?"));
        assertThat(chunks).allMatch(c -> "device-protection".equals(c.productSlug()));
    }
}
