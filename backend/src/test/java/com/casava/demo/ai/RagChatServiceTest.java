package com.casava.demo.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.casava.demo.config.AiProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Flux;

class RagChatServiceTest {

  private VectorStore vectorStore;
  private ChatTokenStreamer streamer;
  private ChatSessionStore sessionStore;
  private AiProperties properties;
  private AccountTools accountTools;
  private GuestPricingTools guestPricingTools;
  private RagChatService service;

  @BeforeEach
  void setUp() {
    vectorStore = mock(VectorStore.class);
    streamer = mock(ChatTokenStreamer.class);
    accountTools = mock(AccountTools.class);
    guestPricingTools = mock(GuestPricingTools.class);
    properties = new AiProperties();
    properties.setTopK(5);
    properties.setSimilarityThreshold(0.55);
    properties.setMaxContextChars(6000);
    properties.setMaxHistoryMessages(20);
    sessionStore = new ChatSessionStore(properties);
    service =
        new RagChatService(
            vectorStore, streamer, sessionStore, properties, accountTools, guestPricingTools);
  }

  @Test
  void retrievesBuildsPromptStreamsAndReturnsCitations() {
    Document exclusion =
        Document.builder()
            .id("exclusion:1")
            .text("Intentional damage is excluded from Device Protection.")
            .metadata(
                Map.of(
                    "title", "Intentional damage",
                    "sourceType", "exclusion",
                    "productName", "Device Protection",
                    "productSlug", "device-protection"))
            .score(0.9)
            .build();

    Document belowThreshold =
        Document.builder()
            .id("faq:noise")
            .text("Unrelated low-score chunk that must be dropped.")
            .metadata(
                Map.of(
                    "title", "Noise",
                    "sourceType", "faq",
                    "productName", "Health Cash",
                    "productSlug", "health-cash"))
            .score(0.2)
            .build();

    when(vectorStore.similaritySearch(any(SearchRequest.class)))
        .thenReturn(List.of(exclusion, belowThreshold));

    AtomicReference<String> capturedUserPrompt = new AtomicReference<>();
    when(streamer.stream(eq(SystemPrompt.TEXT), any(String.class), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              capturedUserPrompt.set(invocation.getArgument(1));
              return Flux.just("Theft is covered.");
            });

    RagChatResult result = service.chat("session-1", "Is theft covered?");

    List<String> tokens = result.tokens().collectList().block();
    assertThat(tokens).containsExactly("Theft is covered.");

    ArgumentCaptor<SearchRequest> searchCaptor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(vectorStore).similaritySearch(searchCaptor.capture());
    SearchRequest request = searchCaptor.getValue();
    assertThat(request.getTopK()).isEqualTo(5);
    assertThat(request.getSimilarityThreshold()).isEqualTo(0.55);
    assertThat(request.getQuery()).isEqualTo("Is theft covered?");

    assertThat(capturedUserPrompt.get()).contains("Intentional damage is excluded");
    assertThat(capturedUserPrompt.get()).contains("Question:");
    assertThat(capturedUserPrompt.get()).contains("Is theft covered?");
    assertThat(capturedUserPrompt.get()).doesNotContain("Unrelated low-score chunk");

    assertThat(result.citations())
        .hasSize(1)
        .first()
        .satisfies(
            c -> {
              assertThat(c.title()).isEqualTo("Intentional damage");
              assertThat(c.sourceType()).isEqualTo("exclusion");
              assertThat(c.productName()).isEqualTo("Device Protection");
              assertThat(c.productSlug()).isEqualTo("device-protection");
            });

    assertThat(sessionStore.getMessages("session-1")).hasSize(2);
  }

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void prependsAuthenticatedPrefixAndPassesAccountToolsWhenLoggedIn() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of()));

    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

    AtomicReference<String> capturedUserPrompt = new AtomicReference<>();
    AtomicReference<Object> capturedTool = new AtomicReference<>();
    when(streamer.stream(eq(SystemPrompt.TEXT), any(String.class), any()))
        .thenAnswer(
            invocation -> {
              capturedUserPrompt.set(invocation.getArgument(1));
              // Varargs: Mockito exposes each tool as a separate argument after the fixed params.
              capturedTool.set(invocation.getArgument(2));
              return Flux.just("You have one policy.");
            });

    service.chat("session-auth", "What's on my policy?");

    assertThat(capturedUserPrompt.get()).startsWith(SystemPrompt.AUTHENTICATED_USER_PREFIX.trim());
    assertThat(capturedTool.get()).isSameAs(accountTools);
  }

  @Test
  void prependsGuestPrefixAndPassesGuestPricingToolsWhenLoggedOut() {
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

    AtomicReference<String> capturedUserPrompt = new AtomicReference<>();
    AtomicReference<Object> capturedTool = new AtomicReference<>();
    when(streamer.stream(eq(SystemPrompt.TEXT), any(String.class), any()))
        .thenAnswer(
            invocation -> {
              capturedUserPrompt.set(invocation.getArgument(1));
              capturedTool.set(invocation.getArgument(2));
              return Flux.just("Demo premium is 4000.");
            });

    service.chat("session-guest", "How much for device protection?");

    assertThat(capturedUserPrompt.get()).startsWith(SystemPrompt.GUEST_USER_PREFIX.trim());
    assertThat(capturedTool.get()).isSameAs(guestPricingTools);
  }
}
