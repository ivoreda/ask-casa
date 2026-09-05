package com.casava.demo.purchase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PolicyResponse(
    UUID id,
    UUID quoteId,
    String productSlug,
    String holderName,
    String holderEmail,
    BigDecimal monthlyPremium,
    BigDecimal coverAmount,
    PolicyStatus status,
    String policyNumber,
    Instant createdAt) {

  public static PolicyResponse from(Policy policy) {
    return new PolicyResponse(
        policy.getId(),
        policy.getQuoteId(),
        policy.getProductSlug(),
        policy.getHolderName(),
        policy.getHolderEmail(),
        policy.getMonthlyPremium(),
        policy.getCoverAmount(),
        policy.getStatus(),
        policy.getPolicyNumber(),
        policy.getCreatedAt());
  }
}
