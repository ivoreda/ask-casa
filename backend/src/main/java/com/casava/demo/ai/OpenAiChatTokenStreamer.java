package com.casava.demo.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class OpenAiChatTokenStreamer implements ChatTokenStreamer {

  private final ChatClient chatClient;

  public OpenAiChatTokenStreamer(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  @Override
  public Flux<String> stream(String systemPrompt, String userPrompt) {
    return chatClient.prompt().system(systemPrompt).user(userPrompt).stream().content();
  }
}
