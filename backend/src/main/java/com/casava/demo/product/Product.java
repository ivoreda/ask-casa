package com.casava.demo.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String slug;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 500)
  private String tagline;

  @Column(name = "monthly_from", nullable = false, precision = 19, scale = 2)
  private BigDecimal monthlyFrom;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "cover_highlights")
  private List<String> coverHighlights = new ArrayList<>();

  @Column(length = 4000)
  private String description;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTagline() {
    return tagline;
  }

  public void setTagline(String tagline) {
    this.tagline = tagline;
  }

  public BigDecimal getMonthlyFrom() {
    return monthlyFrom;
  }

  public void setMonthlyFrom(BigDecimal monthlyFrom) {
    this.monthlyFrom = monthlyFrom;
  }

  public List<String> getCoverHighlights() {
    return coverHighlights;
  }

  public void setCoverHighlights(List<String> coverHighlights) {
    this.coverHighlights = coverHighlights;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
