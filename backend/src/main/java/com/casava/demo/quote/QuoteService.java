package com.casava.demo.quote;

import com.casava.demo.auth.AuthException;
import com.casava.demo.product.Product;
import com.casava.demo.product.ProductRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quote pricing and persistence. Demo pricing / not binding.
 */
@Service
public class QuoteService {

  private final ProductRepository productRepository;
  private final QuoteRepository quoteRepository;

  public QuoteService(ProductRepository productRepository, QuoteRepository quoteRepository) {
    this.productRepository = productRepository;
    this.quoteRepository = quoteRepository;
  }

  /**
   * Prices a quote from product inputs and the product's {@code monthlyFrom} baseline.
   *
   * @throws IllegalArgumentException when inputs are out of range or the product is unknown
   */
  public QuotePrice price(QuoteRequest request, java.math.BigDecimal monthlyFrom) {
    return QuotePricer.price(request, monthlyFrom);
  }

  @Transactional
  public Quote create(UUID userId, QuoteRequest request) {
    Product product =
        productRepository
            .findBySlug(request.productSlug())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Unknown productSlug: " + request.productSlug()));

    QuotePrice priced = price(request, product.getMonthlyFrom());

    Quote quote = new Quote();
    quote.setUserId(userId);
    quote.setProductSlug(product.getSlug());
    quote.setInputs(toInputsMap(request));
    quote.setMonthlyPremium(priced.monthlyPremium());
    quote.setCoverAmount(priced.coverAmount());
    quote.setStatus(QuoteStatus.OPEN);
    quote.setCreatedAt(Instant.now());
    return quoteRepository.save(quote);
  }

  @Transactional(readOnly = true)
  public Quote getOwned(UUID userId, UUID quoteId) {
    Quote quote =
        quoteRepository
            .findById(quoteId)
            .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Quote not found"));
    if (!quote.getUserId().equals(userId)) {
      throw new AuthException(HttpStatus.FORBIDDEN, "Not allowed to access this quote");
    }
    return quote;
  }

  static Map<String, Object> toInputsMap(QuoteRequest request) {
    Map<String, Object> inputs = new LinkedHashMap<>();
    if (request.monthlyIncome() != null) {
      inputs.put("monthlyIncome", request.monthlyIncome());
    }
    if (request.coverMonths() != null) {
      inputs.put("coverMonths", request.coverMonths());
    }
    if (request.dependants() != null) {
      inputs.put("dependants", request.dependants());
    }
    if (request.deviceValue() != null) {
      inputs.put("deviceValue", request.deviceValue());
    }
    return inputs;
  }
}
