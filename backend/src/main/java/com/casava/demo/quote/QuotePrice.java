package com.casava.demo.quote;

import java.math.BigDecimal;

/** Result of deterministic demo quote pricing (not binding). */
public record QuotePrice(BigDecimal coverAmount, BigDecimal factor, BigDecimal monthlyPremium) {}
