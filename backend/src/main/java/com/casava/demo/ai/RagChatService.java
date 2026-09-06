package com.casava.demo.ai;

import com.casava.demo.auth.CurrentUser;
import com.casava.demo.config.AiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class RagChatService {

  private final VectorStore vectorStore;
  private final ChatTokenStreamer streamer;
  private final ChatSessionStore sessionStore;
  private final AiProperties properties;
  private final AccountTools accountTools;

  public RagChatService(
      VectorStore vectorStore,
      ChatTokenStreamer streamer,
      ChatSessionStore sessionStore,
      AiProperties properties,
      AccountTools accountTools) {
    this.vectorStore = vectorStore;
    this.streamer = streamer;
    this.sessionStore = sessionStore;
    this.properties = properties;
    this.accountTools = accountTools;
  }

  public RagChatResult chat(String sessionId, String message) {
    sessionStore.appendUser(sessionId, message);

    SearchRequest request =
        SearchRequest.builder()
            .query(message)
            .topK(properties.getTopK())
            .similarityThreshold(properties.getSimilarityThreshold())
            .build();

    List<Document> retrieved = vectorStore.similaritySearch(request);
    List<Document> accepted = filterByThreshold(retrieved);
    List<Document> contextDocs = truncateToMaxChars(accepted);
    List<Citation> citations = toCitations(contextDocs);

    Object[] tools = CurrentUser.isPresent() ? new Object[] {accountTools} : new Object[0];
    String promptText = buildUserPrompt(contextDocs, message);
    if (CurrentUser.isPresent()) {
      promptText = SystemPrompt.AUTHENTICATED_USER_PREFIX + promptText;
    }

    StringBuilder assistant = new StringBuilder();
    Flux<String> tokens =
        streamer
            .stream(SystemPrompt.TEXT, promptText, tools)
            .doOnNext(assistant::append)
            .doOnComplete(() -> sessionStore.appendAssistant(sessionId, assistant.toString()));

    return new RagChatResult(tokens, citations);
  }

  private List<Document> filterByThreshold(List<Document> docs) {
    double threshold = properties.getSimilarityThreshold();
    List<Document> accepted = new ArrayList<>();
    for (Document doc : docs) {
      Double score = doc.getScore();
      if (score == null || score >= threshold) {
        accepted.add(doc);
      }
    }
    return accepted;
  }

  /** Keep highest-ranked docs first; drop lowest-ranked when over budget. */
  private List<Document> truncateToMaxChars(List<Document> docs) {
    int budget = properties.getMaxContextChars();
    List<Document> kept = new ArrayList<>();
    int used = 0;
    for (Document doc : docs) {
      String text = Objects.requireNonNullElse(doc.getText(), "");
      if (kept.isEmpty() && text.length() > budget) {
        kept.add(doc);
        break;
      }
      if (used + text.length() > budget) {
        break;
      }
      kept.add(doc);
      used += text.length();
    }
    return kept;
  }

  private static String buildUserPrompt(List<Document> contextDocs, String question) {
    StringBuilder context = new StringBuilder();
    for (Document doc : contextDocs) {
      if (!context.isEmpty()) {
        context.append("\n\n");
      }
      Object title = doc.getMetadata().get("title");
      Object productName = doc.getMetadata().get("productName");
      Object sourceType = doc.getMetadata().get("sourceType");
      context
          .append("[")
          .append(productName)
          .append(" / ")
          .append(sourceType)
          .append(" / ")
          .append(title)
          .append("]\n")
          .append(doc.getText());
    }
    return "Context:\n" + context + "\n\nQuestion:\n" + question;
  }

  private static List<Citation> toCitations(List<Document> docs) {
    List<Citation> citations = new ArrayList<>();
    for (Document doc : docs) {
      citations.add(
          new Citation(
              stringMeta(doc, "title"),
              stringMeta(doc, "sourceType"),
              stringMeta(doc, "productName"),
              stringMeta(doc, "productSlug")));
    }
    return List.copyOf(citations);
  }

  private static String stringMeta(Document doc, String key) {
    Object value = doc.getMetadata().get(key);
    return value == null ? null : String.valueOf(value);
  }
}
