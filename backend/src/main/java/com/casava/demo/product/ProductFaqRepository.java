package com.casava.demo.product;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductFaqRepository extends JpaRepository<ProductFaq, UUID> {

  List<ProductFaq> findByProductId(UUID productId);
}
