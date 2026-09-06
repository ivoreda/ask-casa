package com.casava.demo.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.casava.demo.product.Product;
import com.casava.demo.product.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuoteServicePreviewTest {

  private ProductRepository productRepository;
  private QuoteRepository quoteRepository;
  private QuoteService quoteService;

  @BeforeEach
  void setUp() {
    productRepository = mock(ProductRepository.class);
    quoteRepository = mock(QuoteRepository.class);
    quoteService = new QuoteService(productRepository, quoteRepository);
  }

  @Test
  void previewDoesNotTouchRepositoryAndMatchesPricer() {
    Product product = new Product();
    product.setSlug(QuoteRequest.DEVICE_PROTECTION);
    product.setMonthlyFrom(new BigDecimal("2500"));
    when(productRepository.findBySlug(QuoteRequest.DEVICE_PROTECTION))
        .thenReturn(Optional.of(product));

    QuoteRequest request =
        new QuoteRequest(QuoteRequest.DEVICE_PROTECTION, null, null, null, new BigDecimal("800000"));

    QuotePreviewResponse preview = quoteService.preview(request);

    assertThat(preview.productSlug()).isEqualTo(QuoteRequest.DEVICE_PROTECTION);
    assertThat(preview.monthlyPremium()).isEqualByComparingTo(
        QuotePricer.price(request, product.getMonthlyFrom()).monthlyPremium());
    assertThat(preview.coverAmount()).isEqualByComparingTo("800000");
    verifyNoInteractions(quoteRepository);
  }
}
