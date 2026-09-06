package com.casava.demo.claims;

import com.casava.demo.auth.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClaimController {

  private final ClaimService claimService;

  public ClaimController(ClaimService claimService) {
    this.claimService = claimService;
  }

  @PostMapping("/api/claims")
  public ClaimResponse file(@RequestBody ClaimRequest request) {
    UUID userId = CurrentUser.requireUserId();
    Claim claim = claimService.file(userId, request.policyId(), request.description());
    return claimService.toResponse(userId, claim);
  }

  @GetMapping("/api/claims/me")
  public List<ClaimResponse> mine() {
    UUID userId = CurrentUser.requireUserId();
    return claimService.listMineResponses(userId);
  }
}
