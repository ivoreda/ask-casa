package com.casava.demo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casava.demo.claims.Claim;
import com.casava.demo.claims.ClaimResponse;
import com.casava.demo.claims.ClaimService;
import com.casava.demo.claims.ClaimStatus;
import com.casava.demo.purchase.PurchaseService;
import com.casava.demo.quote.Quote;
import com.casava.demo.quote.QuoteRequest;
import com.casava.demo.quote.QuoteService;
import com.casava.demo.quote.QuoteStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AccountToolsTest {

  private QuoteService quoteService;
  private PurchaseService purchaseService;
  private ClaimService claimService;
  private AccountTools tools;
  private UUID userId;

  @BeforeEach
  void setUp() {
    quoteService = mock(QuoteService.class);
    purchaseService = mock(PurchaseService.class);
    claimService = mock(ClaimService.class);
    tools = new AccountTools(quoteService, purchaseService, claimService);
    userId = UUID.randomUUID();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createQuoteDelegatesToQuoteServiceWithCurrentUser() {
    Quote quote = new Quote();
    quote.setId(UUID.randomUUID());
    quote.setUserId(userId);
    quote.setProductSlug(QuoteRequest.DEVICE_PROTECTION);
    quote.setInputs(Map.of("deviceValue", new BigDecimal("800000")));
    quote.setMonthlyPremium(new BigDecimal("4000"));
    quote.setCoverAmount(new BigDecimal("800000"));
    quote.setStatus(QuoteStatus.OPEN);
    quote.setCreatedAt(Instant.parse("2026-09-05T12:00:00Z"));

    when(quoteService.create(eq(userId), org.mockito.ArgumentMatchers.any(QuoteRequest.class)))
        .thenReturn(quote);

    String result =
        tools.createQuote(
            QuoteRequest.DEVICE_PROTECTION, null, null, null, new BigDecimal("800000"));

    ArgumentCaptor<QuoteRequest> requestCaptor = ArgumentCaptor.forClass(QuoteRequest.class);
    verify(quoteService).create(eq(userId), requestCaptor.capture());
    QuoteRequest request = requestCaptor.getValue();
    assertThat(request.productSlug()).isEqualTo(QuoteRequest.DEVICE_PROTECTION);
    assertThat(request.deviceValue()).isEqualByComparingTo("800000");
    assertThat(request.monthlyIncome()).isNull();
    assertThat(request.coverMonths()).isNull();
    assertThat(request.dependants()).isNull();

    assertThat(result).contains("4000");
    assertThat(result).contains("800000");
    assertThat(result).contains(quote.getId().toString());
    assertThat(result.toLowerCase()).contains("demo");
  }

  @Test
  void fileClaimDelegatesToClaimService() {
    UUID policyId = UUID.randomUUID();
    Claim claim = new Claim();
    claim.setId(UUID.randomUUID());
    claim.setPolicyId(policyId);
    claim.setClaimNumber("CLM-ABCDEF12");
    claim.setStatus(ClaimStatus.SUBMITTED);
    claim.setDescription("Phone stolen on the bus.");

    when(claimService.file(eq(userId), eq(policyId), eq("Phone stolen on the bus.")))
        .thenReturn(claim);

    String result = tools.fileClaim(policyId.toString(), "Phone stolen on the bus.");

    verify(claimService).file(userId, policyId, "Phone stolen on the bus.");
    assertThat(result).contains("CLM-ABCDEF12");
    assertThat(result).contains("SUBMITTED");
  }

  @Test
  void listMyClaimsDelegatesToClaimService() {
    UUID policyId = UUID.randomUUID();
    ClaimResponse response =
        new ClaimResponse(
            UUID.randomUUID(),
            policyId,
            "Phone stolen on the bus.",
            "SUBMITTED",
            "CLM-ABCDEF12",
            Instant.parse("2026-09-06T06:00:00Z"),
            "POL-XYZ",
            "device-protection");

    when(claimService.listMineResponses(userId)).thenReturn(List.of(response));

    String result = tools.listMyClaims();

    verify(claimService).listMineResponses(userId);
    assertThat(result).contains("CLM-ABCDEF12");
    assertThat(result).contains("POL-XYZ");
    assertThat(result).contains("device-protection");
  }
}
