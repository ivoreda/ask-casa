package com.casava.demo.product;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductFaqRepository extends JpaRepository<ProductFaq, UUID> {

  List<ProductFaq> findByProductId(UUID productId);

  @Query("select f from ProductFaq f join fetch f.product")
  List<ProductFaq> findAllWithProduct();
}
