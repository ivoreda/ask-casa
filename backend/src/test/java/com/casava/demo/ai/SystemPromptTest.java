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
  void promptMentionsLoginForQuotesAndPolicies() {
    String lower = SystemPrompt.TEXT.toLowerCase();
    assertThat(lower).contains("log in");
    assertThat(lower).contains("register");
    assertThat(lower).contains("quote");
    assertThat(lower).contains("policies");
  }

  @Test
  void promptMentionsDemoPricingAndClaimsUnavailable() {
    String lower = SystemPrompt.TEXT.toLowerCase();
    assertThat(lower).contains("demo pricing");
    assertThat(lower).contains("claim");
    assertThat(lower).contains("not available");
  }

  @Test
  void promptMentionsToolsWhenAuthenticated() {
    String lower = SystemPrompt.TEXT.toLowerCase();
    assertThat(lower).contains("authenticated");
    assertThat(lower).contains("tools");
  }
}
