package com.casava.demo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casava.demo.quote.QuotePreviewResponse;
import com.casava.demo.quote.QuoteRequest;
import com.casava.demo.quote.QuoteService;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GuestPricingToolsTest {

  private QuoteService quoteService;
  private GuestPricingTools tools;

  @BeforeEach
  void setUp() {
    quoteService = mock(QuoteService.class);
    tools = new GuestPricingTools(quoteService);
  }

  @Test
  void previewQuoteDelegatesToQuoteServiceAndReturnsPremium() {
    QuotePreviewResponse preview =
        new QuotePreviewResponse(
            QuoteRequest.DEVICE_PROTECTION,
            Map.of("deviceValue", new BigDecimal("800000")),
            new BigDecimal("4000"),
            new BigDecimal("800000"));

    when(quoteService.preview(any(QuoteRequest.class))).thenReturn(preview);

    String result =
        tools.previewQuote(
            QuoteRequest.DEVICE_PROTECTION, null, null, null, new BigDecimal("800000"));

    ArgumentCaptor<QuoteRequest> requestCaptor = ArgumentCaptor.forClass(QuoteRequest.class);
    verify(quoteService).preview(requestCaptor.capture());
    QuoteRequest request = requestCaptor.getValue();
    assertThat(request.productSlug()).isEqualTo(QuoteRequest.DEVICE_PROTECTION);
    assertThat(request.deviceValue()).isEqualByComparingTo("800000");

    assertThat(result).contains("4000");
    assertThat(result).contains("800000");
    assertThat(result).contains(QuoteRequest.DEVICE_PROTECTION);
    assertThat(result.toLowerCase()).contains("demo");
    assertThat(result.toLowerCase()).contains("log in");
  }
}
