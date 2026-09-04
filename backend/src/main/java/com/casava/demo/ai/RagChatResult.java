package com.casava.demo.ai;

import java.util.List;
import reactor.core.publisher.Flux;

public record RagChatResult(Flux<String> tokens, List<Citation> citations) {}
