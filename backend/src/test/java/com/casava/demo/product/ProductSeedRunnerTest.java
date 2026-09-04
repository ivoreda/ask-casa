package com.casava.demo.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(ProductSeedRunner.class)
class ProductSeedRunnerTest {

  @Autowired private ProductRepository productRepository;
  @Autowired private ProductExclusionRepository exclusionRepository;
  @Autowired private ProductFaqRepository faqRepository;
  @Autowired private ProductSeedRunner seedRunner;

  @Test
  void seedsThreeCasavaShapedProductsWhenCatalogEmpty() throws Exception {
    seedRunner.run(new DefaultApplicationArguments());

    assertThat(productRepository.count()).isEqualTo(3);

    Product income = productRepository.findBySlug("income-protection").orElseThrow();
    assertThat(income.getName()).isEqualTo("Income Protection");
    assertThat(income.getMonthlyFrom()).isEqualByComparingTo(new BigDecimal("500"));
    assertThat(income.getDescription()).containsIgnoringCase("demo");
    assertThat(exclusionRepository.findByProductId(income.getId())).hasSizeGreaterThanOrEqualTo(3);
    assertThat(faqRepository.findByProductId(income.getId())).hasSizeGreaterThanOrEqualTo(3);

    Product health = productRepository.findBySlug("health-cash").orElseThrow();
    assertThat(health.getMonthlyFrom()).isEqualByComparingTo(new BigDecimal("350"));
    assertThat(exclusionRepository.findByProductId(health.getId())).hasSizeGreaterThanOrEqualTo(3);
    assertThat(faqRepository.findByProductId(health.getId())).hasSizeGreaterThanOrEqualTo(3);

    Product device = productRepository.findBySlug("device-protection").orElseThrow();
    assertThat(device.getMonthlyFrom()).isEqualByComparingTo(new BigDecimal("2500"));
    assertThat(device.getCoverHighlights()).isNotEmpty();
    assertThat(exclusionRepository.findByProductId(device.getId())).hasSizeGreaterThanOrEqualTo(3);
    assertThat(faqRepository.findByProductId(device.getId())).hasSizeGreaterThanOrEqualTo(3);
  }

  @Test
  void skipsSeedingWhenProductsAlreadyExist() throws Exception {
    seedRunner.run(new DefaultApplicationArguments());
    long firstCount = productRepository.count();

    seedRunner.run(new DefaultApplicationArguments());

    assertThat(productRepository.count()).isEqualTo(firstCount);
    assertThat(productRepository.count()).isEqualTo(3);
  }
}
