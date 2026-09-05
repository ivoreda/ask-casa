package com.casava.demo.purchase;

import com.casava.demo.auth.AuthException;
import com.casava.demo.auth.User;
import com.casava.demo.auth.UserRepository;
import com.casava.demo.quote.Quote;
import com.casava.demo.quote.QuoteRepository;
import com.casava.demo.quote.QuoteStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

  private final QuoteRepository quoteRepository;
  private final PolicyRepository policyRepository;
  private final UserRepository userRepository;

  public PurchaseService(
      QuoteRepository quoteRepository,
      PolicyRepository policyRepository,
      UserRepository userRepository) {
    this.quoteRepository = quoteRepository;
    this.policyRepository = policyRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public Policy purchase(UUID userId, UUID quoteId) {
    Quote quote =
        quoteRepository
            .findById(quoteId)
            .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Quote not found"));

    if (!quote.getUserId().equals(userId)) {
      throw new AuthException(HttpStatus.FORBIDDEN, "Not allowed to purchase this quote");
    }
    if (quote.getStatus() != QuoteStatus.OPEN) {
      throw new AuthException(HttpStatus.CONFLICT, "Quote already purchased");
    }

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User not found"));

    quote.setStatus(QuoteStatus.PURCHASED);
    quoteRepository.save(quote);

    Policy policy = new Policy();
    policy.setUserId(userId);
    policy.setQuoteId(quote.getId());
    policy.setProductSlug(quote.getProductSlug());
    policy.setHolderName(user.getName());
    policy.setHolderEmail(user.getEmail());
    policy.setMonthlyPremium(quote.getMonthlyPremium());
    policy.setCoverAmount(quote.getCoverAmount());
    policy.setStatus(PolicyStatus.ACTIVE);
    policy.setPolicyNumber(newPolicyNumber());
    policy.setCreatedAt(Instant.now());
    return policyRepository.save(policy);
  }

  @Transactional(readOnly = true)
  public List<Policy> listMine(UUID userId) {
    return policyRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  @Transactional(readOnly = true)
  public Policy getOwned(UUID userId, UUID policyId) {
    Policy policy =
        policyRepository
            .findById(policyId)
            .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Policy not found"));
    if (!policy.getUserId().equals(userId)) {
      throw new AuthException(HttpStatus.FORBIDDEN, "Not allowed to access this policy");
    }
    return policy;
  }

  static String newPolicyNumber() {
    String hex = UUID.randomUUID().toString().replace("-", "");
    return "CSV-" + hex.substring(0, 8).toUpperCase();
  }
}
