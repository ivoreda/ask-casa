package com.casava.demo.product;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductExclusionRepository extends JpaRepository<ProductExclusion, UUID> {

  List<ProductExclusion> findByProductId(UUID productId);
}
