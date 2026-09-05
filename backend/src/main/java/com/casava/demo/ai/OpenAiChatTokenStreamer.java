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
  public Flux<String> stream(String systemPrompt, String userPrompt, Object... tools) {
    ChatClient.ChatClientRequestSpec prompt =
        chatClient.prompt().system(systemPrompt).user(userPrompt);
    if (tools != null && tools.length > 0) {
      prompt = prompt.tools(tools);
    }
    return prompt.stream().content();
  }
}
