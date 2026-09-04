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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;

class RagChatServiceTest {

  private VectorStore vectorStore;
  private ChatTokenStreamer streamer;
  private ChatSessionStore sessionStore;
  private AiProperties properties;
  private RagChatService service;

  @BeforeEach
  void setUp() {
    vectorStore = mock(VectorStore.class);
    streamer = mock(ChatTokenStreamer.class);
    properties = new AiProperties();
    properties.setTopK(5);
    properties.setSimilarityThreshold(0.55);
    properties.setMaxContextChars(6000);
    properties.setMaxHistoryMessages(20);
    sessionStore = new ChatSessionStore(properties);
    service = new RagChatService(vectorStore, streamer, sessionStore, properties);
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
    when(streamer.stream(eq(SystemPrompt.TEXT), any(String.class)))
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
}
