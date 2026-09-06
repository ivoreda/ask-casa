package com.casava.demo.claims;

import java.time.Instant;
import java.util.UUID;

public record ClaimResponse(
    UUID id,
    UUID policyId,
    String description,
    String status,
    String claimNumber,
    Instant createdAt) {

  public static ClaimResponse from(Claim claim) {
    return new ClaimResponse(
        claim.getId(),
        claim.getPolicyId(),
        claim.getDescription(),
        claim.getStatus().name(),
        claim.getClaimNumber(),
        claim.getCreatedAt());
  }
}
