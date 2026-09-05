package com.casava.demo.quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic demo quote formula. Demo pricing / not binding.
 *
 * <p>factor = clamp(1 + (coverAmount - reference) / reference, 1, 3)
 * <br>premium = round(monthlyFrom × factor) with scale 0 HALF_UP
 */
final class QuotePricer {

  static final BigDecimal REFERENCE_INCOME = new BigDecimal("500000");
  static final BigDecimal REFERENCE_HEALTH = new BigDecimal("500000");
  static final BigDecimal REFERENCE_DEVICE = new BigDecimal("500000");
  static final BigDecimal HEALTH_BASE_COVER = new BigDecimal("500000");
  static final BigDecimal DEVICE_MAX_COVER = new BigDecimal("1500000");
  static final BigDecimal MIN_FACTOR = BigDecimal.ONE;
  static final BigDecimal MAX_FACTOR = new BigDecimal("3");

  private static final Map<String, BigDecimal> REFERENCE_COVER =
      Map.of(
          QuoteRequest.INCOME_PROTECTION, REFERENCE_INCOME,
          QuoteRequest.HEALTH_CASH, REFERENCE_HEALTH,
          QuoteRequest.DEVICE_PROTECTION, REFERENCE_DEVICE);

  private QuotePricer() {}

  static QuotePrice price(QuoteRequest request, BigDecimal monthlyFrom) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(monthlyFrom, "monthlyFrom");
    if (monthlyFrom.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("monthlyFrom must be positive");
    }
    if (request.productSlug() == null || request.productSlug().isBlank()) {
      throw new IllegalArgumentException("productSlug is required");
    }

    BigDecimal coverAmount = coverAmount(request);
    BigDecimal reference = REFERENCE_COVER.get(request.productSlug());
    if (reference == null) {
      throw new IllegalArgumentException("Unknown productSlug: " + request.productSlug());
    }

    BigDecimal factor = factor(coverAmount, reference);
    BigDecimal premium =
        monthlyFrom.multiply(factor).setScale(0, RoundingMode.HALF_UP);

    return new QuotePrice(coverAmount, factor, premium);
  }

  private static BigDecimal coverAmount(QuoteRequest request) {
    return switch (request.productSlug()) {
      case QuoteRequest.INCOME_PROTECTION -> incomeCover(request);
      case QuoteRequest.HEALTH_CASH -> healthCover(request);
      case QuoteRequest.DEVICE_PROTECTION -> deviceCover(request);
      default ->
          throw new IllegalArgumentException("Unknown productSlug: " + request.productSlug());
    };
  }

  private static BigDecimal incomeCover(QuoteRequest request) {
    if (request.monthlyIncome() == null || request.monthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("monthlyIncome must be positive");
    }
    if (request.coverMonths() == null || request.coverMonths() < 3 || request.coverMonths() > 12) {
      throw new IllegalArgumentException("coverMonths must be between 3 and 12");
    }
    return request.monthlyIncome().multiply(BigDecimal.valueOf(request.coverMonths()));
  }

  private static BigDecimal healthCover(QuoteRequest request) {
    if (request.dependants() == null || request.dependants() < 0 || request.dependants() > 4) {
      throw new IllegalArgumentException("dependants must be between 0 and 4");
    }
    BigDecimal multiplier =
        BigDecimal.ONE.add(new BigDecimal("0.25").multiply(BigDecimal.valueOf(request.dependants())));
    return HEALTH_BASE_COVER.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
  }

  private static BigDecimal deviceCover(QuoteRequest request) {
    if (request.deviceValue() == null || request.deviceValue().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("deviceValue must be positive");
    }
    return request.deviceValue().min(DEVICE_MAX_COVER);
  }

  /** factor = clamp(1.0 + (coverAmount - reference) / reference, 1.0, 3.0) */
  private static BigDecimal factor(BigDecimal coverAmount, BigDecimal reference) {
    BigDecimal raw =
        BigDecimal.ONE.add(
            coverAmount
                .subtract(reference)
                .divide(reference, 10, RoundingMode.HALF_UP));
    if (raw.compareTo(MIN_FACTOR) < 0) {
      return MIN_FACTOR;
    }
    if (raw.compareTo(MAX_FACTOR) > 0) {
      return MAX_FACTOR;
    }
    return raw.stripTrailingZeros();
  }
}
