# Casava Demo — Ask Casa AI Design

**Date:** 2026-09-03  
**Status:** Approved for implementation planning  
**Inspiration:** [Casava enjoy-staging](https://enjoy-staging.casava.com)

## Goal

Build a demo insurance product shell whose primary deliverable is **Ask Casa**: an AI assistant grounded in the company’s own product knowledge via **RAG** (retrieve from an indexed company knowledge base, then generate with OpenRouter). Quote and claims flows are stubbed. Auth and personal policy lookup are explicitly out of scope for v1.

## Success criteria

- A visitor can open a chat-first UI and ask product questions about Income Protection, Health Cash, and Device Protection.
- Answers are grounded in retrieved chunks from the company knowledge base (seeded from H2 product data), not generic model knowledge.
- The UI can show which sources/chunks were used (product name / FAQ / exclusion) for demo credibility.
- Replies stream token-by-token over SSE.
- Questions about “my policy / my claim / my payout” are refused with a register/login stub message.
- Quote and File a claim are placeholder CTAs only.

## Non-goals (v1)

- Real authentication or registration
- Customer policies, claims, or payouts in the assistant
- LLM tool/function calling as the primary knowledge path
- Payments, underwriting engine, partner APIs
- Pixel-perfect clone of the Casava marketing site

## Stack

| Layer | Choice |
|-------|--------|
| API | Java, Spring Boot **4.1.1** (latest stable), Spring AI |
| LLM + embeddings | OpenRouter (chat model + embedding model) |
| DB | **H2** file-based (`backend/data/casava-db`) — no Docker |
| Vectors | Spring AI **SimpleVectorStore** (`backend/data/vector-store.json`) |
| Frontend | Vite + React + TypeScript |
| Streaming | Server-Sent Events (`text/event-stream`) |

**Why Spring:** Credible insurance-style backend (REST, DB, streaming, RAG pipeline). Spring AI integrates with OpenRouter and vector stores. Acceptable trade-off vs a Node-only toy demo.

## Architecture

Monorepo:

```
casava-demo/
  backend/              Spring Boot app (H2 + SimpleVectorStore under backend/data/)
  frontend/             Vite + React chat shell
  docs/superpowers/specs/
```

```
Browser (Vite/React)
    │  POST /api/chat (SSE)
    ▼
Spring Boot
    ├── System rules (short: refuse personal account Qs; answer only from context)
    ├── Embed user query → similarity search (SimpleVectorStore)
    ├── Build prompt with top-k chunks + citations metadata
    ├── Spring AI chat → OpenRouter (stream tokens)
    └── Source tables: products, exclusions, FAQs → indexed Documents in vector store
```

**Ingest path (on seed / reindex):**

1. Read structured company data from `products`, `product_exclusions`, `product_faqs` (H2).
2. Chunk into retrieval units (e.g. one FAQ pair, one exclusion, one “product overview” block).
3. Embed via OpenRouter embeddings API; store documents + metadata in SimpleVectorStore and persist to `vector-store.json`.

## Grounding strategy (RAG)

### RAG (primary)

Per user message:

1. Embed the query.
2. Retrieve **top-k** (default **5**) chunks by cosine similarity, optionally filtered by product if the query names one.
3. Inject chunks into the user/system context with clear source labels.
4. Instruct the model: answer **only** from provided context; if missing, say the knowledge base doesn’t cover it — do not invent cover, prices, or exclusions.
5. Stream the completion; attach citation metadata on the `done` event for the UI.

### System rules (thin, not CAG-as-knowledge)

A short fixed system prompt for behavior only:

- Product information assistant for this demo insurer
- Not legal/financial advice
- Personal policy/claim/payout questions → refuse; ask user to register (stub)
- Prefer citing product names from retrieved context

No full product digest in the prompt — knowledge comes from retrieval.

### Why RAG here (vs tools/CAG)

- Demonstrates a production-shaped **company knowledge base** pattern (index once, retrieve many).
- Scales when FAQs, exclusions, and longer product copy grow without stuffing the prompt.
- Enables **citations** (“from Device Protection exclusions”) for stakeholder demos.
- Structured H2 catalog remains the editorial source of truth; the vector index is derived.

Tool calling and a large CAG digest are **out of scope for v1**; they can return later for live account lookups after auth.

## Data model (v1)

### `products`

- `id`, `slug`, `name`, `tagline`
- `monthly_from` (numeric)
- `cover_highlights` (JSON array of short strings, or a related table)
- `description` (longer marketing/product text)

### `product_exclusions`

- `id`, `product_id`, `text`

### `product_faqs`

- `id`, `product_id`, `question`, `answer`

### Vector store documents (SimpleVectorStore)

Each chunk is a Spring AI `Document` with:

- `content` — chunk text embedded and retrieved
- metadata: `title`, `sourceType` (`product_overview` | `exclusion` | `faq`), `sourceId`, `productSlug`, `productName`

Persisted to `backend/data/vector-store.json` (not a separate SQL embedding table).

### Seed products

1. **Income Protection** — income replacement, demo limits/premiums  
2. **Health Cash** — hospital/emergency cash benefits  
3. **Device Protection** — phone/laptop/tablet cover  

Numbers may be Casava-shaped demo values; labeled as demo data. Seed must leave enough FAQ/exclusion/overview text that retrieval has something meaningful to find.

## Backend components

- **`product`** — entities, repositories, seed runner for catalog tables
- **`knowledge`** — chunk builder, SimpleVectorStore reindex + save (run after seed)
- **`ai`** — chat controller (SSE), retrieve → prompt → stream, session/history store (in-memory for demo)
- **`config`** — CORS for Vite origin, OpenRouter chat + embedding model ids, datasource

### Chat API (sketch)

- `POST /api/chat`  
  - Body: `{ "sessionId": string, "message": string }` (server keeps short per-session history)  
  - Response: SSE events, e.g. `status` (“searching knowledge…”), `token`, `done` (includes `citations[]`), `error`

### Caps

- Max history messages retained per session: **20**
- Top-k chunks per turn: **5**
- Max chunk characters injected: **~6k** total (truncate lowest-ranked if needed)
- Upstream timeout: **60s** → SSE `error` event
- Similarity floor: drop chunks below a configured score threshold (avoid stuffing irrelevant noise)

## Frontend components

- Brand + one-line value prop + primary CTA into chat  
- Chat panel: message list, streaming assistant bubble, suggested prompts  
- Optional citation chips under an answer (product / source type)  
- Stub nav: Get a quote / File a claim → “Register to continue” (no auth)  
- Friendly handling of SSE errors and missing API key / backend down

## Error handling

| Case | Behavior |
|------|----------|
| Missing OpenRouter key | HTTP 503 / SSE error; UI friendly message |
| Rate limit / timeout | SSE error; UI “Try again” |
| Empty retrieval | Model told context is empty; should say knowledge base has no match |
| Embedding / vector search failure | SSE error; admit search failed — do not answer from raw model memory |
| Personal policy question | Refusal via system rules |

No stack traces or secrets in client responses. No intentional PII collection beyond free-text chat.

## Testing

- **Unit:** chunk builder from product/FAQ/exclusion rows; citation metadata mapping; refusal wording for personal questions  
- **Integration:** reindex writes embeddings (mock embedder OK); chat retrieves expected chunk for a known FAQ query; streaming path  
- **Manual script:** limits, exclusions, compare products, “what’s on my policy?”, unknown product, citation display

## Future extensions (not v1)

- Auth + persona/session binding to real policies  
- Tool calls for policy status and claims (alongside RAG for docs)  
- Larger document ingest (PDF policy wordings, claims playbooks)  
- Hybrid CAG briefing for routing only  
- Quote calculator and claims file stubs becoming real flows  

## Decisions log

| Topic | Decision |
|-------|----------|
| Demo focus | Ask Casa first; stub quote/claims |
| Knowledge source | Company DB → chunked/embedded knowledge base |
| Personal data | Out of scope until registration/auth |
| Grounding | **RAG (SimpleVectorStore) primary**; thin system rules only |
| Local infra | **No Docker** — H2 file DB + JSON vector store |
| UI | Chat-first shell |
| Frontend | Vite + React + TypeScript |
| Backend | Spring Boot 4.1.1 + Spring AI + OpenRouter |
| Streaming | Yes (SSE) |
| Products | Income Protection, Health Cash, Device Protection |
| Overall approach | Monolith Spring + thin React |
| Vector store | Spring AI SimpleVectorStore (file-backed) |
