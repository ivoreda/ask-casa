package com.casava.demo.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policies")
public class Policy {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "quote_id", nullable = false, unique = true)
  private UUID quoteId;

  @Column(name = "product_slug", nullable = false, length = 100)
  private String productSlug;

  @Column(name = "holder_name", nullable = false, length = 200)
  private String holderName;

  @Column(name = "holder_email", nullable = false, length = 320)
  private String holderEmail;

  @Column(name = "monthly_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal monthlyPremium;

  @Column(name = "cover_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal coverAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PolicyStatus status;

  @Column(name = "policy_number", nullable = false, unique = true, length = 40)
  private String policyNumber;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UUID getQuoteId() {
    return quoteId;
  }

  public void setQuoteId(UUID quoteId) {
    this.quoteId = quoteId;
  }

  public String getProductSlug() {
    return productSlug;
  }

  public void setProductSlug(String productSlug) {
    this.productSlug = productSlug;
  }

  public String getHolderName() {
    return holderName;
  }

  public void setHolderName(String holderName) {
    this.holderName = holderName;
  }

  public String getHolderEmail() {
    return holderEmail;
  }

  public void setHolderEmail(String holderEmail) {
    this.holderEmail = holderEmail;
  }

  public BigDecimal getMonthlyPremium() {
    return monthlyPremium;
  }

  public void setMonthlyPremium(BigDecimal monthlyPremium) {
    this.monthlyPremium = monthlyPremium;
  }

  public BigDecimal getCoverAmount() {
    return coverAmount;
  }

  public void setCoverAmount(BigDecimal coverAmount) {
    this.coverAmount = coverAmount;
  }

  public PolicyStatus getStatus() {
    return status;
  }

  public void setStatus(PolicyStatus status) {
    this.status = status;
  }

  public String getPolicyNumber() {
    return policyNumber;
  }

  public void setPolicyNumber(String policyNumber) {
    this.policyNumber = policyNumber;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
