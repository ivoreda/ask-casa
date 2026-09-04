package com.casava.demo.ai;

import com.casava.demo.config.AiProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatSessionStore {

  private final ConcurrentHashMap<String, List<Message>> sessions = new ConcurrentHashMap<>();
  private final AiProperties properties;

  public ChatSessionStore(AiProperties properties) {
    this.properties = properties;
  }

  public void appendUser(String sessionId, String content) {
    append(sessionId, new UserMessage(content));
  }

  public void appendAssistant(String sessionId, String content) {
    append(sessionId, new AssistantMessage(content));
  }

  public List<Message> getMessages(String sessionId) {
    List<Message> messages = sessions.get(sessionId);
    if (messages == null) {
      return List.of();
    }
    synchronized (messages) {
      return List.copyOf(messages);
    }
  }

  private void append(String sessionId, Message message) {
    List<Message> messages =
        sessions.computeIfAbsent(sessionId, id -> Collections.synchronizedList(new ArrayList<>()));
    synchronized (messages) {
      messages.add(message);
      int max = properties.getMaxHistoryMessages();
      while (messages.size() > max) {
        messages.removeFirst();
      }
    }
  }
}
