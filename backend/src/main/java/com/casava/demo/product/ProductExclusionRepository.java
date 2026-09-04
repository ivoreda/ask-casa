package com.casava.demo.product;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductExclusionRepository extends JpaRepository<ProductExclusion, UUID> {

  List<ProductExclusion> findByProductId(UUID productId);

  @Query("select e from ProductExclusion e join fetch e.product")
  List<ProductExclusion> findAllWithProduct();
}
