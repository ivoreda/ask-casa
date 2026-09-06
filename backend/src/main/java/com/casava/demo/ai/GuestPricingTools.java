package com.casava.demo.ai;

import com.casava.demo.quote.QuotePreviewResponse;
import com.casava.demo.quote.QuoteRequest;
import com.casava.demo.quote.QuoteService;
import java.math.BigDecimal;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class GuestPricingTools {

  private final QuoteService quoteService;

  public GuestPricingTools(QuoteService quoteService) {
    this.quoteService = quoteService;
  }

  @Tool(
      description =
          "Preview a demo insurance premium without saving a quote. "
              + "Use when the user is not logged in or only wants a price. "
              + "Tell them to log in / register to buy. Demo pricing only, not binding. "
              + "Product slugs: income-protection (monthlyIncome + coverMonths 3-12), "
              + "health-cash (dependants 0-4), device-protection (deviceValue).")
  public String previewQuote(
      @ToolParam(description = "Product slug") String productSlug,
      @ToolParam(required = false, description = "Monthly income for income-protection")
          BigDecimal monthlyIncome,
      @ToolParam(required = false, description = "Cover months 3-12") Integer coverMonths,
      @ToolParam(required = false, description = "Dependants 0-4") Integer dependants,
      @ToolParam(required = false, description = "Device value") BigDecimal deviceValue) {
    try {
      QuoteRequest request =
          new QuoteRequest(productSlug, monthlyIncome, coverMonths, dependants, deviceValue);
      QuotePreviewResponse preview = quoteService.preview(request);
      return "Demo price preview (not saved, not binding). product="
          + preview.productSlug()
          + " monthlyPremium="
          + preview.monthlyPremium()
          + " coverAmount="
          + preview.coverAmount()
          + ". Tell the user to register or log in to buy.";
    } catch (Exception ex) {
      return "Could not preview quote: "
          + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
    }
  }
}
