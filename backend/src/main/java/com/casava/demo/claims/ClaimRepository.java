package com.casava.demo.claims;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

  List<Claim> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
