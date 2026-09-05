package com.casava.demo.quote;

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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "quotes")
public class Quote {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "product_slug", nullable = false, length = 100)
  private String productSlug;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private Map<String, Object> inputs = new HashMap<>();

  @Column(name = "monthly_premium", nullable = false, precision = 19, scale = 2)
  private BigDecimal monthlyPremium;

  @Column(name = "cover_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal coverAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private QuoteStatus status;

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

  public String getProductSlug() {
    return productSlug;
  }

  public void setProductSlug(String productSlug) {
    this.productSlug = productSlug;
  }

  public Map<String, Object> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, Object> inputs) {
    this.inputs = inputs;
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

  public QuoteStatus getStatus() {
    return status;
  }

  public void setStatus(QuoteStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
