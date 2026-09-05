package com.casava.demo.purchase;

import com.casava.demo.auth.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PolicyController {

  private final PurchaseService purchaseService;

  public PolicyController(PurchaseService purchaseService) {
    this.purchaseService = purchaseService;
  }

  @PostMapping("/api/purchases")
  public PolicyResponse purchase(@RequestBody PurchaseRequest request) {
    UUID userId = CurrentUser.requireUserId();
    if (request == null || request.quoteId() == null) {
      throw new IllegalArgumentException("quoteId is required");
    }
    return PolicyResponse.from(purchaseService.purchase(userId, request.quoteId()));
  }

  @GetMapping("/api/policies/me")
  public List<PolicyResponse> myPolicies() {
    UUID userId = CurrentUser.requireUserId();
    return purchaseService.listMine(userId).stream().map(PolicyResponse::from).toList();
  }

  @GetMapping("/api/policies/{id}")
  public PolicyResponse get(@PathVariable UUID id) {
    UUID userId = CurrentUser.requireUserId();
    return PolicyResponse.from(purchaseService.getOwned(userId, id));
  }
}
