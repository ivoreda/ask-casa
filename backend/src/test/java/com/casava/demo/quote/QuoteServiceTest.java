package com.casava.demo.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.casava.demo.product.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuoteServiceTest {

  private QuoteService quoteService;

  @BeforeEach
  void setUp() {
    // price() is pure; repos unused in these formula unit tests
    quoteService = new QuoteService(null, null);
  }

  @Test
  void pricesIncomeProtection() {
    QuoteRequest request =
        QuoteRequest.incomeProtection(new BigDecimal("150000"), 6);
    QuotePrice price = quoteService.price(request, new BigDecimal("500"));

    assertThat(price.coverAmount()).isEqualByComparingTo("900000");
    assertThat(price.factor()).isEqualByComparingTo("1.8");
    assertThat(price.monthlyPremium()).isEqualByComparingTo("900");
  }

  @Test
  void pricesIncomeProtectionClampsFactorAtFloor() {
    QuoteRequest request =
        QuoteRequest.incomeProtection(new BigDecimal("50000"), 6);
    QuotePrice price = quoteService.price(request, new BigDecimal("500"));

    assertThat(price.coverAmount()).isEqualByComparingTo("300000");
    assertThat(price.factor()).isEqualByComparingTo("1.0");
    assertThat(price.monthlyPremium()).isEqualByComparingTo("500");
  }

  @Test
  void pricesHealthCashWithDependants() {
    QuoteRequest request = QuoteRequest.healthCash(2);
    QuotePrice price = quoteService.price(request, new BigDecimal("350"));

    assertThat(price.coverAmount()).isEqualByComparingTo("750000");
    assertThat(price.factor()).isEqualByComparingTo("1.5");
    assertThat(price.monthlyPremium()).isEqualByComparingTo("525");
  }

  @Test
  void pricesHealthCashWithZeroDependants() {
    QuoteRequest request = QuoteRequest.healthCash(0);
    QuotePrice price = quoteService.price(request, new BigDecimal("350"));

    assertThat(price.coverAmount()).isEqualByComparingTo("500000");
    assertThat(price.factor()).isEqualByComparingTo("1.0");
    assertThat(price.monthlyPremium()).isEqualByComparingTo("350");
  }

  @Test
  void pricesHealthCashWithMaxDependants() {
    QuoteRequest request = QuoteRequest.healthCash(4);
    QuotePrice price = quoteService.price(request, new BigDecimal("350"));

    assertThat(price.coverAmount()).isEqualByComparingTo("1000000");
    assertThat(price.factor()).isEqualByComparingTo("2.0");
    assertThat(price.monthlyPremium()).isEqualByComparingTo("700");
  }

  @Test
  void pricesDeviceProtection() {
    QuoteRequest request =
        QuoteRequest.deviceProtection(new BigDecimal("800000"));
    QuotePrice price = quoteService.price(request, new BigDecimal("2500"));

    assertThat(price.coverAmount()).isEqualByComparingTo("800000");
    assertThat(price.factor()).isEqualByComparingTo("1.6");
    assertThat(price.monthlyPremium()).isEqualByComparingTo("4000");
  }

  @Test
  void pricesDeviceProtectionCapsCoverAndFactor() {
    QuoteRequest request =
        QuoteRequest.deviceProtection(new BigDecimal("2000000"));
    QuotePrice price = quoteService.price(request, new BigDecimal("2500"));

    assertThat(price.coverAmount()).isEqualByComparingTo("1500000");
    assertThat(price.factor()).isEqualByComparingTo("3.0");
    assertThat(price.monthlyPremium()).isEqualByComparingTo("7500");
  }

  @Test
  void rejectsInvalidCoverMonths() {
    assertThatThrownBy(
            () ->
                quoteService.price(
                    QuoteRequest.incomeProtection(new BigDecimal("150000"), 2),
                    new BigDecimal("500")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("coverMonths");

    assertThatThrownBy(
            () ->
                quoteService.price(
                    QuoteRequest.incomeProtection(new BigDecimal("150000"), 13),
                    new BigDecimal("500")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("coverMonths");
  }

  @Test
  void rejectsInvalidMonthlyIncome() {
    assertThatThrownBy(
            () ->
                quoteService.price(
                    QuoteRequest.incomeProtection(new BigDecimal("0"), 6),
                    new BigDecimal("500")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("monthlyIncome");
  }

  @Test
  void rejectsInvalidDependants() {
    assertThatThrownBy(
            () -> quoteService.price(QuoteRequest.healthCash(-1), new BigDecimal("350")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dependants");

    assertThatThrownBy(
            () -> quoteService.price(QuoteRequest.healthCash(5), new BigDecimal("350")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dependants");
  }

  @Test
  void rejectsInvalidDeviceValue() {
    assertThatThrownBy(
            () ->
                quoteService.price(
                    QuoteRequest.deviceProtection(new BigDecimal("0")),
                    new BigDecimal("2500")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("deviceValue");
  }

  @Test
  void rejectsUnknownProductSlug() {
    assertThatThrownBy(
            () ->
                quoteService.price(
                    new QuoteRequest("unknown-product", null, null, null, null),
                    new BigDecimal("100")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("productSlug");
  }
}
