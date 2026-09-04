# Ask Casa RAG Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a chat-first Casava-style demo where Ask Casa answers product questions via RAG (Postgres product data → embeddings → pgvector → OpenRouter streaming), with quote/claims stubbed and no auth.

**Architecture:** Monorepo with Spring Boot 4.1.1 + Spring AI 2.x backend (JPA catalog, chunk indexer into Spring AI `VectorStore`/pgvector, SSE chat) and a Vite + React + TypeScript frontend. Structured tables remain the editorial source of truth; the vector index is derived on seed/reindex.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring AI 2.0.x (OpenAI-compatible client → OpenRouter), Postgres + pgvector, Vite, React, TypeScript

**Spec:** `docs/superpowers/specs/2026-09-03-casava-demo-ai-design.md`

---

## File structure

```
casava-demo/
  docker-compose.yml
  .gitignore
  README.md
  backend/
    pom.xml
    src/main/java/com/casava/demo/
      CasavaDemoApplication.java
      config/CorsConfig.java
      config/AiProperties.java
      product/
        Product.java
        ProductExclusion.java
        ProductFaq.java
        ProductRepository.java
        ProductExclusionRepository.java
        ProductFaqRepository.java
        ProductSeedRunner.java
      knowledge/
        KnowledgeChunkDocument.java          # record: content + metadata
        KnowledgeChunkBuilder.java           # catalog rows → chunk docs
        KnowledgeReindexService.java         # clear + embed + vectorStore.add
      ai/
        SystemPrompt.java
        ChatSessionStore.java
        Citation.java
        ChatRequest.java
        RagChatService.java
        ChatController.java
    src/main/resources/application.yml
    src/test/java/com/casava/demo/
      knowledge/KnowledgeChunkBuilderTest.java
      ai/SystemPromptTest.java
      ai/RagChatServiceTest.java
      ai/ChatControllerTest.java
  frontend/
    package.json
    vite.config.ts
    index.html
    src/main.tsx
    src/App.tsx
    src/styles.css
    src/api/chat.ts
    src/components/ChatPanel.tsx
    src/components/MessageList.tsx
    src/components/StubModal.tsx
```

**Note on vectors:** Spring AI PgVectorStore owns the `vector_store` table. Chunk metadata (`productSlug`, `productName`, `sourceType`, `sourceId`, `title`) lives in document metadata — same intent as the spec’s `knowledge_chunks` without a hand-rolled embedding column.

---

### Task 1: Repo scaffolding (Docker, backend, frontend shells)

**Files:**
- Create: `docker-compose.yml`, `.gitignore`, `README.md`
- Create: `backend/pom.xml`, `backend/src/main/java/com/casava/demo/CasavaDemoApplication.java`, `backend/src/main/resources/application.yml`
- Create: `frontend/package.json`, `frontend/vite.config.ts`, `frontend/index.html`, `frontend/src/main.tsx`, `frontend/src/App.tsx`, `frontend/src/styles.css`

- [ ] **Step 1: Add root `.gitignore`**

```gitignore
.idea/
.vscode/
*.iml
.DS_Store
.env
.env.local
backend/target/
frontend/node_modules/
frontend/dist/
```

- [ ] **Step 2: Add `docker-compose.yml`**

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg17
    environment:
      POSTGRES_DB: casava
      POSTGRES_USER: casava
      POSTGRES_PASSWORD: casava
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U casava -d casava"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  pgdata:
```

- [ ] **Step 3: Create Spring Boot backend `pom.xml`**

Use Spring Boot **4.1.1**, Java **21**, Spring AI BOM **2.0.0** (or latest 2.0.x patch), dependencies:

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-validation`
- `org.postgresql:postgresql`
- `spring-ai-starter-model-openai`
- `spring-ai-starter-vector-store-pgvector`
- `spring-boot-starter-test` (test)

Include `spring-ai-bom` in `dependencyManagement`. Main class package: `com.casava.demo`.

- [ ] **Step 4: Add minimal application class and `application.yml`**

```yaml
spring:
  application:
    name: casava-demo
  datasource:
    url: jdbc:postgresql://localhost:5432/casava
    username: casava
    password: casava
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate.jdbc.time_zone: UTC
  ai:
    openai:
      api-key: ${OPENROUTER_API_KEY:}
      base-url: https://openrouter.ai/api/v1
      chat:
        options:
          model: ${OPENROUTER_CHAT_MODEL:openai/gpt-4o-mini}
      embedding:
        options:
          model: ${OPENROUTER_EMBEDDING_MODEL:openai/text-embedding-3-small}
    vectorstore:
      pgvector:
        index-type: HNSW
        distance-type: COSINE_DISTANCE
        dimensions: 1536
        initialize-schema: true

casava:
  ai:
    top-k: 5
    similarity-threshold: 0.55
    max-context-chars: 6000
    max-history-messages: 20
    frontend-origin: http://localhost:5173

server:
  port: 8080
```

If Spring AI 2.x property names differ (`chat.model` flattened), adjust to match the BOM docs at implement time — keep OpenRouter `base-url` including `/v1`.

- [ ] **Step 5: Scaffold Vite React TS frontend**

```bash
cd /Users/enyatastaff/dev/casava-demo
npm create vite@latest frontend -- --template react-ts
cd frontend && npm install
```

Set `vite.config.ts` proxy optional; prefer calling `http://localhost:8080` directly with CORS.

Replace `App.tsx` with a placeholder heading “Casava · Ask Casa”.

- [ ] **Step 6: Add README with run instructions**

Document: Docker up → set `OPENROUTER_API_KEY` → `./mvnw spring-boot:run` in `backend` → `npm run dev` in `frontend`.

- [ ] **Step 7: Verify Postgres comes up**

```bash
docker compose up -d
docker compose ps
```

Expected: `postgres` healthy.

- [ ] **Step 8: Commit**

```bash
git add .gitignore docker-compose.yml README.md backend frontend
git commit -m "chore: scaffold monorepo, Postgres, Spring Boot, and Vite app"
```

---

### Task 2: Product catalog entities, repos, and seed data

**Files:**
- Create: `backend/src/main/java/com/casava/demo/product/*.java`
- Test: `backend/src/test/java/com/casava/demo/product/ProductSeedRunnerTest.java` (optional; prefer repository slice later)
- Prefer unit-testing chunk builder in Task 3; here focus on entities + seed that loads three products with rich text

- [ ] **Step 1: Create JPA entities**

`Product`: `id` (UUID), `slug`, `name`, `tagline`, `monthlyFrom` (`BigDecimal`), `coverHighlights` (`List<String>` via `@JdbcTypeCode(SqlTypes.JSON)` or `@ElementCollection`), `description` (`@Column(length = 4000)`).

`ProductExclusion`: `id`, `@ManyToOne Product product`, `text`.

`ProductFaq`: `id`, `@ManyToOne Product product`, `question`, `answer`.

- [ ] **Step 2: Create Spring Data repositories**

`ProductRepository extends JpaRepository<Product, UUID>` with `Optional<Product> findBySlug(String slug)` and `boolean existsBySlug(String slug)`.

Same pattern for exclusion/faq repos with `List<...> findByProductId(UUID productId)`.

- [ ] **Step 3: Implement `ProductSeedRunner` (`ApplicationRunner`)**

If any product exists, skip. Otherwise insert:

1. **income-protection** — from ₦500/mo, up to ₦240,000/month, 6 months cover; ≥3 exclusions; ≥3 FAQs  
2. **health-cash** — from ₦350/mo, up to ₦500,000/incident; ≥3 exclusions; ≥3 FAQs  
3. **device-protection** — from ₦2,500/mo, up to ₦1,500,000; ≥3 exclusions; ≥3 FAQs  

Use Casava-shaped demo copy; mark descriptions as demo data.

- [ ] **Step 4: Boot against Docker Postgres and confirm seed**

```bash
cd backend && ./mvnw -q spring-boot:run
# in another terminal:
docker compose exec postgres psql -U casava -d casava -c "select slug, name from products;"
```

Expected: three rows. Stop the app after check.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/casava/demo/product
git commit -m "feat: add product catalog entities and Casava-shaped seed data"
```

---

### Task 3: Knowledge chunk builder (TDD)

**Files:**
- Create: `backend/src/main/java/com/casava/demo/knowledge/KnowledgeChunkDocument.java`
- Create: `backend/src/main/java/com/casava/demo/knowledge/KnowledgeChunkBuilder.java`
- Test: `backend/src/test/java/com/casava/demo/knowledge/KnowledgeChunkBuilderTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.casava.demo.knowledge;

import com.casava.demo.product.Product;
import com.casava.demo.product.ProductExclusion;
import com.casava.demo.product.ProductFaq;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkBuilderTest {

    @Test
    void buildsOverviewExclusionAndFaqChunks() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSlug("device-protection");
        product.setName("Device Protection");
        product.setTagline("Phones and laptops covered");
        product.setMonthlyFrom(new BigDecimal("2500"));
        product.setCoverHighlights(List.of("No deductibles", "Up to ₦1,500,000"));
        product.setDescription("Cover for theft, drop, and water damage.");

        ProductExclusion exclusion = new ProductExclusion();
        exclusion.setId(UUID.randomUUID());
        exclusion.setProduct(product);
        exclusion.setText("Pre-existing damage is not covered.");

        ProductFaq faq = new ProductFaq();
        faq.setId(UUID.randomUUID());
        faq.setProduct(product);
        faq.setQuestion("How fast is replacement?");
        faq.setAnswer("Typically within 24–72 hours after approval.");

        KnowledgeChunkBuilder builder = new KnowledgeChunkBuilder();
        List<KnowledgeChunkDocument> chunks = builder.build(List.of(product), List.of(exclusion), List.of(faq));

        assertThat(chunks).hasSize(3);
        assertThat(chunks).anyMatch(c -> "product_overview".equals(c.sourceType()) && c.content().contains("Device Protection"));
        assertThat(chunks).anyMatch(c -> "exclusion".equals(c.sourceType()) && c.content().contains("Pre-existing"));
        assertThat(chunks).anyMatch(c -> "faq".equals(c.sourceType()) && c.content().contains("How fast is replacement?"));
        assertThat(chunks).allMatch(c -> "device-protection".equals(c.productSlug()));
    }
}
```

- [ ] **Step 2: Run test — expect fail**

```bash
cd backend && ./mvnw -q test -Dtest=KnowledgeChunkBuilderTest
```

Expected: FAIL (classes missing).

- [ ] **Step 3: Implement records + builder**

```java
public record KnowledgeChunkDocument(
    String content,
    String title,
    String sourceType,
    String sourceId,
    String productSlug,
    String productName
) {}
```

Builder rules:

- One overview chunk per product: name, tagline, monthly from, highlights, description  
- One chunk per exclusion: titled `"{name} exclusion"`  
- One chunk per FAQ: `"Q: ...\nA: ..."` titled with the question  

- [ ] **Step 4: Re-run test — expect pass**

```bash
cd backend && ./mvnw -q test -Dtest=KnowledgeChunkBuilderTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/casava/demo/knowledge backend/src/test/java/com/casava/demo/knowledge
git commit -m "feat: build RAG chunks from product catalog rows"
```

---

### Task 4: Knowledge reindex into pgvector

**Files:**
- Create: `backend/src/main/java/com/casava/demo/knowledge/KnowledgeReindexService.java`
- Modify: `ProductSeedRunner` to call reindex after seed (or separate `ApplicationRunner` ordered after seed)
- Test: `backend/src/test/java/com/casava/demo/knowledge/KnowledgeReindexServiceTest.java` with mocked `VectorStore` + `EmbeddingModel` if needed

- [ ] **Step 1: Write failing unit test for mapping to Spring AI `Document`s**

```java
@Test
void reindexAddsDocumentsToVectorStore() {
    VectorStore vectorStore = mock(VectorStore.class);
    KnowledgeChunkBuilder builder = new KnowledgeChunkBuilder();
    // stub repos to return one product + exclusion + faq
    KnowledgeReindexService service = new KnowledgeReindexService(productRepo, exclusionRepo, faqRepo, builder, vectorStore);

    service.reindex();

    verify(vectorStore).delete(anyList()); // or delete-all strategy used
    ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
    verify(vectorStore).add(captor.capture());
    assertThat(captor.getValue()).isNotEmpty();
    assertThat(captor.getValue().getFirst().getMetadata()).containsKeys("sourceType", "productSlug", "title");
}
```

Adapt delete API to whatever Spring AI 2 `VectorStore` exposes (`delete(List<String> ids)` vs filter). If no delete-all, delete by collecting existing IDs or drop/recreate via `initialize-schema` only on empty DB for demo — document the chosen approach in code comments.

- [ ] **Step 2: Run test — expect fail**

```bash
cd backend && ./mvnw -q test -Dtest=KnowledgeReindexServiceTest
```

- [ ] **Step 3: Implement `KnowledgeReindexService`**

For each `KnowledgeChunkDocument`, create:

```java
Document doc = new Document(
    chunk.content(),
    Map.of(
        "title", chunk.title(),
        "sourceType", chunk.sourceType(),
        "sourceId", chunk.sourceId(),
        "productSlug", chunk.productSlug(),
        "productName", chunk.productName()
    )
);
```

Call `vectorStore.add(documents)` after clear. Log chunk count.

- [ ] **Step 4: Wire runner after seed**

On startup: seed catalog if empty → always reindex if vector store empty OR if `casava.knowledge.reindex-on-startup=true` (default `true` for demo).

- [ ] **Step 5: Manual smoke (requires `OPENROUTER_API_KEY`)**

```bash
export OPENROUTER_API_KEY=sk-or-...
cd backend && ./mvnw -q spring-boot:run
```

Expected logs: seeded products + “Indexed N knowledge chunks”.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/casava/demo/knowledge backend/src/test/java/com/casava/demo/knowledge
git commit -m "feat: reindex product knowledge into pgvector"
```

---

### Task 5: System prompt, session store, RAG chat service (TDD)

**Files:**
- Create: `backend/src/main/java/com/casava/demo/ai/*.java` (except controller)
- Create: `backend/src/main/java/com/casava/demo/config/AiProperties.java`
- Test: `SystemPromptTest.java`, `RagChatServiceTest.java`

- [ ] **Step 1: Write `SystemPromptTest`**

Assert prompt text contains: answer only from context; not legal advice; refuse personal policy/claim/payout and mention registration.

- [ ] **Step 2: Implement `SystemPrompt` constant/helper** — make test pass.

- [ ] **Step 3: Write `RagChatServiceTest` with mocks**

Mock `VectorStore.similaritySearch` to return one `Document` about Device Protection exclusion. Mock `ChatClient` / `ChatModel` streaming to emit `"Theft is covered."` (use Spring AI test doubles or a thin `ChatStreamer` interface you own to keep tests simple).

Assert:

- Search called with topK=5  
- Prompt user message includes retrieved content  
- Returned citations include `title` + `sourceType` + `productName`  
- Chunks below similarity threshold are dropped  

Recommended seam:

```java
public interface ChatTokenStreamer {
    Flux<String> stream(String systemPrompt, String userPrompt);
}
```

Production impl wraps Spring AI `ChatClient`; tests use a fake.

- [ ] **Step 4: Implement `AiProperties` `@ConfigurationProperties(prefix = "casava.ai")`**

Fields: `topK`, `similarityThreshold`, `maxContextChars`, `maxHistoryMessages`, `frontendOrigin`.

- [ ] **Step 5: Implement `ChatSessionStore`**

In-memory `ConcurrentHashMap<String, List<ChatMessage>>`, trim to `maxHistoryMessages`.

- [ ] **Step 6: Implement `RagChatService`**

Flow:

1. Append user message to session  
2. `similaritySearch(SearchRequest.builder().query(message).topK(topK).similarityThreshold(threshold).build())`  
3. Truncate concatenated chunk text to `maxContextChars` (drop lowest-ranked first)  
4. Build user prompt: `Context:\n...\n\nQuestion:\n...`  
5. Stream tokens via `ChatTokenStreamer`  
6. On complete, append assistant message; return citations list  

`Citation` record: `title`, `sourceType`, `productName`, `productSlug`.

- [ ] **Step 7: Run tests**

```bash
cd backend && ./mvnw -q test -Dtest=SystemPromptTest,RagChatServiceTest
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/casava/demo/ai backend/src/main/java/com/casava/demo/config backend/src/test/java/com/casava/demo/ai
git commit -m "feat: add RAG retrieve-and-generate chat service"
```

---

### Task 6: SSE chat API + CORS + missing-key handling

**Files:**
- Create: `ChatController.java`, `ChatRequest.java`, `CorsConfig.java`
- Test: `ChatControllerTest.java`

- [ ] **Step 1: Write controller test with `@WebMvcTest`**

POST `/api/chat` with JSON `{"sessionId":"s1","message":"What is covered?"}`  

Mock `RagChatService` to return a `Flux` of SSE-shaped events or have controller map service stream.

Expect:

- `Content-Type` starts with `text/event-stream`  
- Body contains event names `status`, `token`, `done`  

Second test: when API key blank, respond with SSE `error` or 503 — match `application.yml` check in an `@Component OpenRouterHealth` or controller guard.

- [ ] **Step 2: Implement DTOs and controller**

```java
public record ChatRequest(
    @NotBlank String sessionId,
    @NotBlank String message
) {}
```

```java
@PostMapping(value = "/api/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody ChatRequest request) { ... }
```

Event payloads (JSON strings in `data:`):

- `status`: `{"type":"status","message":"searching knowledge…"}`  
- `token`: `{"type":"token","text":"..."}`  
- `done`: `{"type":"done","citations":[...]}`  
- `error`: `{"type":"error","message":"..."}`  

On retrieval failure, emit `error` and complete (do not call chat model).

- [ ] **Step 3: CORS config** allow `casava.ai.frontend-origin`, methods GET/POST/OPTIONS, headers `*`.

- [ ] **Step 4: Run tests**

```bash
cd backend && ./mvnw -q test -Dtest=ChatControllerTest
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/casava/demo/ai backend/src/main/java/com/casava/demo/config backend/src/test/java/com/casava/demo/ai
git commit -m "feat: expose streaming SSE chat endpoint with CORS"
```

---

### Task 7: Frontend chat shell with citations and stubs

**Files:**
- Create/modify: `frontend/src/api/chat.ts`, `ChatPanel.tsx`, `MessageList.tsx`, `StubModal.tsx`, `App.tsx`, `styles.css`

- [ ] **Step 1: Implement SSE client `chat.ts`**

```typescript
export type Citation = {
  title: string;
  sourceType: string;
  productName: string;
  productSlug: string;
};

export type ChatHandlers = {
  onStatus?: (message: string) => void;
  onToken: (text: string) => void;
  onDone: (citations: Citation[]) => void;
  onError: (message: string) => void;
};

export async function streamChat(
  sessionId: string,
  message: string,
  handlers: ChatHandlers,
  signal?: AbortSignal
): Promise<void> {
  const res = await fetch("http://localhost:8080/api/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
    body: JSON.stringify({ sessionId, message }),
    signal,
  });
  if (!res.ok || !res.body) {
    handlers.onError(`Chat failed (${res.status})`);
    return;
  }
  // parse SSE: split on \n\n, read event/data lines, JSON.parse data
}
```

Persist `sessionId` in `sessionStorage` (generate UUID once).

- [ ] **Step 2: Build UI**

`App.tsx` composition:

- Header: brand **Casava**, links “Get a quote” / “File a claim” → open `StubModal` (“Register to continue”)  
- Hero line: “Insurance, finally enjoyable.” + short supporting sentence  
- Primary focus: `ChatPanel` with suggested prompts:
  - “What’s covered under Device Protection?”
  - “Compare Income Protection and Health Cash”
  - “What are exclusions for Health Cash?”
  - “What’s on my policy?” (refusal demo)

`MessageList`: user/assistant bubbles; streaming assistant appends tokens; after `done`, show citation chips (`productName` · `sourceType`).

- [ ] **Step 3: Styles**

CSS variables for a clear brand direction (avoid purple-gradient / cream-serif clichés). Full-viewport chat-first layout; works on mobile. Light motion: fade-in messages, subtle typing cursor while streaming.

- [ ] **Step 4: Manual UI check**

```bash
cd frontend && npm run dev
```

Expected: page loads, stubs open modal, suggested prompts fill the input.

- [ ] **Step 5: Commit**

```bash
git add frontend
git commit -m "feat: add Ask Casa chat UI with citations and register stubs"
```

---

### Task 8: End-to-end verification + polish

**Files:**
- Modify: `README.md` (manual test script)
- Optional: `backend/src/test/resources/application-test.yml` if needed for CI unit tests without DB

- [ ] **Step 1: Run full backend tests**

```bash
cd backend && ./mvnw test
```

Expected: all PASS.

- [ ] **Step 2: Run stack**

```bash
docker compose up -d
export OPENROUTER_API_KEY=...
cd backend && ./mvnw spring-boot:run
cd frontend && npm run dev
```

- [ ] **Step 3: Manual script (from spec)**

| Prompt | Expect |
|--------|--------|
| Device Protection limits / theft | Grounded answer + citations |
| Health Cash exclusions | Exclusion chunks cited |
| Compare Income vs Health | Uses multiple product chunks |
| “What’s on my policy?” | Refusal + register |
| “Do you cover spaceships?” | Knowledge base doesn’t cover / no invent |
| Citation chips | Visible under answer |

- [ ] **Step 4: Update README** with the manual script and env vars (`OPENROUTER_API_KEY`, optional model overrides).

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: add runbook and manual RAG verification script"
```

---

## Self-review (plan vs spec)

| Spec requirement | Task |
|------------------|------|
| Spring Boot 4.1.1 + Spring AI + OpenRouter | 1 |
| Postgres + pgvector | 1, 4 |
| Product seed IP / Health / Device | 2 |
| Chunk from overview/exclusion/FAQ | 3 |
| Embed + index | 4 |
| RAG retrieve top-5, threshold, max context | 5 |
| Thin system rules / personal refusal | 5 |
| SSE streaming + citations on done | 6–7 |
| Chat-first UI + quote/claim stubs | 7 |
| Error handling missing key / search fail | 6–7 |
| Unit + integration-style tests | 3–6 |
| Manual script | 8 |

No CAG digest or tool-calling in v1 (matches updated spec).

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-04-ask-casa-rag-demo.md`. Two execution options:

1. **Subagent-Driven (recommended)** — fresh subagent per task, review between tasks  
2. **Inline Execution** — execute tasks in this session with executing-plans checkpoints  

Which approach?
