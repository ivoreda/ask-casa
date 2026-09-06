package com.casava.demo.ai;

import com.casava.demo.auth.CurrentUser;
import com.casava.demo.claims.Claim;
import com.casava.demo.claims.ClaimResponse;
import com.casava.demo.claims.ClaimService;
import com.casava.demo.purchase.Policy;
import com.casava.demo.purchase.PurchaseService;
import com.casava.demo.quote.Quote;
import com.casava.demo.quote.QuoteRequest;
import com.casava.demo.quote.QuoteService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Ask Casa tools for authenticated quote, policy, and claim actions. Demo only. */
@Component
public class AccountTools {

  private final QuoteService quoteService;
  private final PurchaseService purchaseService;
  private final ClaimService claimService;

  public AccountTools(
      QuoteService quoteService, PurchaseService purchaseService, ClaimService claimService) {
    this.quoteService = quoteService;
    this.purchaseService = purchaseService;
    this.claimService = claimService;
  }

  @Tool(
      description =
          "Create a demo insurance quote for the logged-in user. "
              + "Product slugs: income-protection (needs monthlyIncome + coverMonths 3-12), "
              + "health-cash (needs dependants 0-4), device-protection (needs deviceValue). "
              + "Pricing is demo only and not binding.")
  public String createQuote(
      @ToolParam(description = "Product slug") String productSlug,
      @ToolParam(required = false, description = "Monthly income for income-protection")
          BigDecimal monthlyIncome,
      @ToolParam(required = false, description = "Cover months 3-12 for income-protection")
          Integer coverMonths,
      @ToolParam(required = false, description = "Dependants 0-4 for health-cash") Integer dependants,
      @ToolParam(required = false, description = "Device value for device-protection")
          BigDecimal deviceValue) {
    try {
      UUID userId = CurrentUser.requireUserId();
      QuoteRequest request =
          new QuoteRequest(productSlug, monthlyIncome, coverMonths, dependants, deviceValue);
      Quote quote = quoteService.create(userId, request);
      return "Demo quote created (not binding). id="
          + quote.getId()
          + " product="
          + quote.getProductSlug()
          + " monthlyPremium="
          + quote.getMonthlyPremium()
          + " coverAmount="
          + quote.getCoverAmount()
          + " status="
          + quote.getStatus()
          + ". Tell the user they can buy it via Get a quote → Continue to buy.";
    } catch (Exception ex) {
      return "Could not create quote: " + message(ex);
    }
  }

  @Tool(description = "List the logged-in user's demo policies. Call this for 'what's on my policy'.")
  public String listMyPolicies() {
    try {
      UUID userId = CurrentUser.requireUserId();
      List<Policy> policies = purchaseService.listMine(userId);
      if (policies.isEmpty()) {
        return "You have no policies yet. Offer to help them get a quote.";
      }
      return policies.stream().map(AccountTools::summarizePolicy).collect(Collectors.joining("\n"));
    } catch (Exception ex) {
      return "Could not list policies: " + message(ex);
    }
  }

  @Tool(description = "Get one of the logged-in user's demo policies by policy id")
  public String getPolicy(@ToolParam(description = "Policy UUID") String policyId) {
    try {
      UUID userId = CurrentUser.requireUserId();
      Policy policy = purchaseService.getOwned(userId, UUID.fromString(policyId));
      return summarizePolicy(policy);
    } catch (Exception ex) {
      return "Could not get policy: " + message(ex);
    }
  }

  @Tool(
      description =
          "File a demo claim against one of the user's policies. "
              + "Requires policyId (from listMyPolicies) and a short incident description.")
  public String fileClaim(
      @ToolParam(description = "Policy UUID to claim against") String policyId,
      @ToolParam(description = "What happened / claim description") String description) {
    try {
      UUID userId = CurrentUser.requireUserId();
      Claim claim = claimService.file(userId, UUID.fromString(policyId), description);
      return "Demo claim submitted. claimNumber="
          + claim.getClaimNumber()
          + " policyId="
          + claim.getPolicyId()
          + " status="
          + claim.getStatus()
          + " description="
          + claim.getDescription();
    } catch (Exception ex) {
      return "Could not file claim: " + message(ex);
    }
  }

  @Tool(
      description =
          "List the logged-in user's demo claims (newest first). "
              + "Call this for claim history and when summarizing policies so you can mention claims.")
  public String listMyClaims() {
    try {
      UUID userId = CurrentUser.requireUserId();
      List<ClaimResponse> claims = claimService.listMineResponses(userId);
      if (claims.isEmpty()) {
        return "You have no claims yet. Offer to help them file a claim if they have a policy.";
      }
      return claims.stream().map(AccountTools::summarizeClaim).collect(Collectors.joining("\n"));
    } catch (Exception ex) {
      return "Could not list claims: " + message(ex);
    }
  }

  private static String summarizePolicy(Policy policy) {
    return "policyId="
        + policy.getId()
        + " policyNumber="
        + policy.getPolicyNumber()
        + " product="
        + policy.getProductSlug()
        + " monthlyPremium="
        + policy.getMonthlyPremium()
        + " coverAmount="
        + policy.getCoverAmount()
        + " status="
        + policy.getStatus();
  }

  private static String summarizeClaim(ClaimResponse claim) {
    return "claimNumber="
        + claim.claimNumber()
        + " status="
        + claim.status()
        + " policyId="
        + claim.policyId()
        + " policyNumber="
        + claim.policyNumber()
        + " product="
        + claim.productSlug()
        + " description="
        + claim.description();
  }

  private static String message(Exception ex) {
    return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
  }
}
