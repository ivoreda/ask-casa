package com.casava.demo.quote;

import java.math.BigDecimal;
import java.util.Map;

public record QuotePreviewResponse(
    String productSlug,
    Map<String, Object> inputs,
    BigDecimal monthlyPremium,
    BigDecimal coverAmount) {}
