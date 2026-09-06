package com.casava.demo.claims;

import com.casava.demo.auth.AuthException;
import com.casava.demo.purchase.Policy;
import com.casava.demo.purchase.PurchaseService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ClaimService {

  private final ClaimRepository claimRepository;
  private final PurchaseService purchaseService;

  public ClaimService(ClaimRepository claimRepository, PurchaseService purchaseService) {
    this.claimRepository = claimRepository;
    this.purchaseService = purchaseService;
  }

  @Transactional
  public Claim file(UUID userId, UUID policyId, String description) {
    if (!StringUtils.hasText(description) || description.trim().length() < 10) {
      throw new AuthException(
          HttpStatus.BAD_REQUEST, "Please describe what happened (at least 10 characters)");
    }
    Policy policy = purchaseService.getOwned(userId, policyId);

    Claim claim = new Claim();
    claim.setUserId(userId);
    claim.setPolicyId(policy.getId());
    claim.setDescription(description.trim());
    claim.setStatus(ClaimStatus.SUBMITTED);
    claim.setClaimNumber(newClaimNumber());
    claim.setCreatedAt(Instant.now());
    return claimRepository.save(claim);
  }

  @Transactional(readOnly = true)
  public List<Claim> listMine(UUID userId) {
    return claimRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  @Transactional(readOnly = true)
  public List<ClaimResponse> listMineResponses(UUID userId) {
    return listMine(userId).stream().map(c -> toResponse(userId, c)).toList();
  }

  public ClaimResponse toResponse(UUID userId, Claim claim) {
    try {
      Policy policy = purchaseService.getOwned(userId, claim.getPolicyId());
      return ClaimResponse.from(claim, policy.getPolicyNumber(), policy.getProductSlug());
    } catch (RuntimeException ex) {
      return ClaimResponse.from(claim, null, null);
    }
  }

  static String newClaimNumber() {
    String hex = UUID.randomUUID().toString().replace("-", "");
    return "CLM-" + hex.substring(0, 8).toUpperCase();
  }
}
