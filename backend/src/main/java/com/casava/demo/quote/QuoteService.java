package com.casava.demo.quote;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Quote pricing and (later) persistence. Demo pricing / not binding.
 */
@Service
public class QuoteService {

  /**
   * Prices a quote from product inputs and the product's {@code monthlyFrom} baseline.
   *
   * @throws IllegalArgumentException when inputs are out of range or the product is unknown
   */
  public QuotePrice price(QuoteRequest request, BigDecimal monthlyFrom) {
    return QuotePricer.price(request, monthlyFrom);
  }
}
