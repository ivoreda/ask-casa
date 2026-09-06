package com.casava.demo.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
    boolean withTools = tools != null && tools.length > 0;
    if (withTools) {
      prompt = prompt.tools(tools);
      // Tool-calling loops are reliable on .call(); streaming tool args is flaky across providers.
      String content = prompt.call().content();
      if (!StringUtils.hasText(content)) {
        return Flux.empty();
      }
      return Flux.just(content);
    }
    return prompt.stream().content();
  }
}
