package com.casava.demo.claims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casava.demo.auth.AuthException;
import com.casava.demo.purchase.Policy;
import com.casava.demo.purchase.PurchaseService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClaimServiceTest {

  private ClaimRepository claimRepository;
  private PurchaseService purchaseService;
  private ClaimService claimService;
  private UUID userId;
  private UUID policyId;

  @BeforeEach
  void setUp() {
    claimRepository = mock(ClaimRepository.class);
    purchaseService = mock(PurchaseService.class);
    claimService = new ClaimService(claimRepository, purchaseService);
    userId = UUID.randomUUID();
    policyId = UUID.randomUUID();
  }

  @Test
  void filesClaimAgainstOwnedPolicy() {
    Policy policy = new Policy();
    policy.setId(policyId);
    when(purchaseService.getOwned(userId, policyId)).thenReturn(policy);
    when(claimRepository.save(any(Claim.class)))
        .thenAnswer(
            inv -> {
              Claim c = inv.getArgument(0);
              c.setId(UUID.randomUUID());
              return c;
            });

    Claim saved =
        claimService.file(userId, policyId, "Phone stolen from bag while commuting to work.");

    ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
    verify(claimRepository).save(captor.capture());
    Claim claim = captor.getValue();
    assertThat(claim.getUserId()).isEqualTo(userId);
    assertThat(claim.getPolicyId()).isEqualTo(policyId);
    assertThat(claim.getStatus()).isEqualTo(ClaimStatus.SUBMITTED);
    assertThat(claim.getClaimNumber()).startsWith("CLM-");
    assertThat(saved.getId()).isNotNull();
  }

  @Test
  void rejectsShortDescription() {
    assertThatThrownBy(() -> claimService.file(userId, policyId, "too short"))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining("10 characters");
  }

  @Test
  void listMineDelegatesToRepository() {
    when(claimRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
    assertThat(claimService.listMine(userId)).isEmpty();
    verify(claimRepository).findByUserIdOrderByCreatedAtDesc(eq(userId));
  }

  @Test
  void listMineResponsesIncludesPolicyNumberAndProductSlug() {
    UUID claimId = UUID.randomUUID();
    Claim claim = new Claim();
    claim.setId(claimId);
    claim.setUserId(userId);
    claim.setPolicyId(policyId);
    claim.setDescription("Phone stolen from bag while commuting to work.");
    claim.setStatus(ClaimStatus.SUBMITTED);
    claim.setClaimNumber("CLM-TEST0001");
    claim.setCreatedAt(Instant.parse("2026-09-06T06:00:00Z"));

    Policy policy = new Policy();
    policy.setId(policyId);
    policy.setPolicyNumber("POL-ABC12345");
    policy.setProductSlug("device-protection");

    when(claimRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(claim));
    when(purchaseService.getOwned(userId, policyId)).thenReturn(policy);

    List<ClaimResponse> responses = claimService.listMineResponses(userId);

    assertThat(responses).hasSize(1);
    ClaimResponse r = responses.getFirst();
    assertThat(r.claimNumber()).isEqualTo("CLM-TEST0001");
    assertThat(r.policyId()).isEqualTo(policyId);
    assertThat(r.policyNumber()).isEqualTo("POL-ABC12345");
    assertThat(r.productSlug()).isEqualTo("device-protection");
  }

  @Test
  void listMineResponsesSurvivesMissingPolicy() {
    Claim claim = new Claim();
    claim.setId(UUID.randomUUID());
    claim.setUserId(userId);
    claim.setPolicyId(policyId);
    claim.setDescription("Phone stolen from bag while commuting to work.");
    claim.setStatus(ClaimStatus.SUBMITTED);
    claim.setClaimNumber("CLM-ORPHAN01");
    claim.setCreatedAt(Instant.now());

    when(claimRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(claim));
    when(purchaseService.getOwned(userId, policyId))
        .thenThrow(new AuthException(HttpStatus.NOT_FOUND, "Policy not found"));

    List<ClaimResponse> responses = claimService.listMineResponses(userId);

    assertThat(responses).hasSize(1);
    assertThat(responses.getFirst().policyNumber()).isNull();
    assertThat(responses.getFirst().productSlug()).isNull();
  }
}
