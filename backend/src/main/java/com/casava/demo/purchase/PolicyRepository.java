package com.casava.demo.purchase;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, UUID> {

  List<Policy> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
