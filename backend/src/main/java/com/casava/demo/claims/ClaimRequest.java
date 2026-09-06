package com.casava.demo.claims;

import java.util.UUID;

public record ClaimRequest(UUID policyId, String description) {}
