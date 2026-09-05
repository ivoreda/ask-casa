package com.casava.demo.ai;

import reactor.core.publisher.Flux;

public interface ChatTokenStreamer {

  Flux<String> stream(String systemPrompt, String userPrompt, Object... tools);
}
