package com.casava.demo.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SystemPromptTest {

  @Test
  void promptRequiresProductFactsFromContext() {
    String lower = SystemPrompt.TEXT.toLowerCase();
    assertThat(lower).contains("context");
    assertThat(lower).contains("do not invent");
  }

  @Test
  void promptStatesNotLegalAdvice() {
    assertThat(SystemPrompt.TEXT.toLowerCase()).contains("not legal");
  }

  @Test
  void promptMentionsLoginForAccountActions() {
    String lower = SystemPrompt.TEXT.toLowerCase();
    assertThat(lower).contains("log in");
    assertThat(lower).contains("register");
    assertThat(lower).contains("quote");
    assertThat(lower).contains("policies");
  }

  @Test
  void promptRequiresToolsForPersonalPolicyQuestions() {
    String lower = SystemPrompt.TEXT.toLowerCase();
    assertThat(lower).contains("listmypolicies");
    assertThat(lower).contains("must call");
    assertThat(lower).contains("demo pricing");
  }

  @Test
  void authenticatedPrefixMentionsAccountTools() {
    String lower = SystemPrompt.AUTHENTICATED_USER_PREFIX.toLowerCase();
    assertThat(lower).contains("logged in");
    assertThat(lower).contains("listmypolicies");
    assertThat(lower).contains("createquote");
    assertThat(lower).contains("fileclaim");
  }

  @Test
  void promptRequiresListMyClaimsForClaimAndPolicyQuestions() {
    String lower = SystemPrompt.TEXT.toLowerCase();
    assertThat(lower).contains("listmyclaims");
    assertThat(lower).contains("claim history");
  }

  @Test
  void authenticatedPrefixMentionsListMyClaims() {
    String lower = SystemPrompt.AUTHENTICATED_USER_PREFIX.toLowerCase();
    assertThat(lower).contains("listmyclaims");
    assertThat(lower).contains("fileclaim");
  }

  @Test
  void promptMentionsGuestPreviewQuoteGuidance() {
    String lower = SystemPrompt.TEXT.toLowerCase();
    assertThat(lower).contains("previewquote");
    assertThat(lower).contains("do not invent premiums");
    assertThat(lower).contains("buying");
  }

  @Test
  void guestPrefixMentionsPreviewQuote() {
    String lower = SystemPrompt.GUEST_USER_PREFIX.toLowerCase();
    assertThat(lower).contains("not logged in");
    assertThat(lower).contains("previewquote");
    assertThat(lower).contains("register");
  }
}
