package com.casava.demo.quote;

import java.math.BigDecimal;

/**
 * Quote inputs for demo pricing. Use factory helpers so product-specific fields are explicit.
 * Demo pricing / not binding.
 */
public record QuoteRequest(
    String productSlug,
    BigDecimal monthlyIncome,
    Integer coverMonths,
    Integer dependants,
    BigDecimal deviceValue) {

  public static final String INCOME_PROTECTION = "income-protection";
  public static final String HEALTH_CASH = "health-cash";
  public static final String DEVICE_PROTECTION = "device-protection";

  public static QuoteRequest incomeProtection(BigDecimal monthlyIncome, int coverMonths) {
    return new QuoteRequest(INCOME_PROTECTION, monthlyIncome, coverMonths, null, null);
  }

  public static QuoteRequest healthCash(int dependants) {
    return new QuoteRequest(HEALTH_CASH, null, null, dependants, null);
  }

  public static QuoteRequest deviceProtection(BigDecimal deviceValue) {
    return new QuoteRequest(DEVICE_PROTECTION, null, null, null, deviceValue);
  }
}
