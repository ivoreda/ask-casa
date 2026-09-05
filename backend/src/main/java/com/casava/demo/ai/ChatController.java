package com.casava.demo.ai;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.ObjectMapper;

@RestController
public class ChatController {

  private final RagChatService ragChatService;
  private final ObjectMapper objectMapper;
  private final String apiKey;

  public ChatController(
      RagChatService ragChatService,
      ObjectMapper objectMapper,
      @Value("${spring.ai.openai.api-key:}") String apiKey) {
    this.ragChatService = ragChatService;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
  }

  @PostMapping(value = "/api/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
    SseEmitter emitter = new SseEmitter(0L);
    // Virtual threads do not inherit ThreadLocal SecurityContext — copy auth explicitly.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    Thread.startVirtualThread(
        () -> {
          try {
            if (authentication != null) {
              SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            if (!StringUtils.hasText(apiKey)) {
              send(
                  emitter,
                  "error",
                  Map.of(
                      "type",
                      "error",
                      "message",
                      "OPENROUTER_API_KEY is missing. Set the environment variable or spring.ai.openai.api-key property."));
              emitter.complete();
              return;
            }

            send(
                emitter,
                "status",
                Map.of("type", "status", "message", "searching knowledge…"));

            RagChatResult result = ragChatService.chat(request.sessionId(), request.message());

            result
                .tokens()
                .toStream()
                .forEach(
                    token -> {
                      try {
                        send(emitter, "token", Map.of("type", "token", "text", token));
                      } catch (IOException e) {
                        throw new IllegalStateException("Failed to emit token event", e);
                      }
                    });

            Map<String, Object> done = new LinkedHashMap<>();
            done.put("type", "done");
            done.put("citations", result.citations());
            send(emitter, "done", done);
            emitter.complete();
          } catch (Exception ex) {
            try {
              send(
                  emitter,
                  "error",
                  Map.of(
                      "type",
                      "error",
                      "message",
                      ex.getMessage() != null ? ex.getMessage() : "chat failed"));
              emitter.complete();
            } catch (Exception sendError) {
              emitter.completeWithError(sendError);
            }
          } finally {
            SecurityContextHolder.clearContext();
          }
        });

    return emitter;
  }

  private void send(SseEmitter emitter, String eventName, Object payload) throws IOException {
    String json = objectMapper.writeValueAsString(payload);
    emitter.send(SseEmitter.event().name(eventName).data(json));
  }
}
