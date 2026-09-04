package com.casava.demo.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SystemPromptTest {

  @Test
  void promptRequiresAnsweringOnlyFromContext() {
    assertThat(SystemPrompt.TEXT.toLowerCase()).contains("only from");
    assertThat(SystemPrompt.TEXT.toLowerCase()).contains("context");
  }

  @Test
  void promptStatesNotLegalAdvice() {
    assertThat(SystemPrompt.TEXT.toLowerCase()).contains("not legal");
  }

  @Test
  void promptRefusesPersonalPolicyClaimPayoutAndMentionsRegistration() {
    String lower = SystemPrompt.TEXT.toLowerCase();
    assertThat(lower).contains("policy");
    assertThat(lower).contains("claim");
    assertThat(lower).contains("payout");
    assertThat(lower).contains("register");
  }
}
