package com.casava.demo.quote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record QuoteResponse(
    UUID id,
    String productSlug,
    Map<String, Object> inputs,
    BigDecimal monthlyPremium,
    BigDecimal coverAmount,
    QuoteStatus status,
    Instant createdAt) {

  public static QuoteResponse from(Quote quote) {
    return new QuoteResponse(
        quote.getId(),
        quote.getProductSlug(),
        quote.getInputs(),
        quote.getMonthlyPremium(),
        quote.getCoverAmount(),
        quote.getStatus(),
        quote.getCreatedAt());
  }
}
