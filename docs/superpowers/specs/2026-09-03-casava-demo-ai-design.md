# Casava Demo — Ask Casa AI Design

**Date:** 2026-09-03  
**Status:** Approved for implementation planning  
**Inspiration:** [Casava enjoy-staging](https://enjoy-staging.casava.com)

## Goal

Build a demo insurance product shell whose primary deliverable is **Ask Casa**: an AI assistant grounded in the company’s own product database via OpenRouter. Quote and claims flows are stubbed. Auth and personal policy lookup are explicitly out of scope for v1.

## Success criteria

- A visitor can open a chat-first UI and ask product questions about Income Protection, Health Cash, and Device Protection.
- Answers are grounded in Postgres-backed catalog data (CAG digest + tool calls), not generic model knowledge.
- Replies stream token-by-token over SSE.
- Questions about “my policy / my claim / my payout” are refused with a register/login stub message.
- Quote and File a claim are placeholder CTAs only.

## Non-goals (v1)

- Real authentication or registration
- Customer policies, claims, or payouts in the assistant
- RAG / vector store
- Payments, underwriting engine, partner APIs
- Pixel-perfect clone of the Casava marketing site

## Stack

| Layer | Choice |
|-------|--------|
| API | Java, Spring Boot **4.1.1** (latest stable), Spring AI |
| LLM | OpenRouter (OpenAI-compatible) |
| DB | Postgres (Docker Compose) |
| Frontend | Vite + React + TypeScript |
| Streaming | Server-Sent Events (`text/event-stream`) |

**Why Spring:** Credible insurance-style backend (REST, tools, DB, streaming). Spring AI integrates with OpenRouter. Acceptable trade-off vs a Node-only toy demo.

## Architecture

Monorepo:

```
casava-demo/
  backend/              Spring Boot app
  frontend/             Vite + React chat shell
  docker-compose.yml    Postgres
  docs/superpowers/specs/
```

```
Browser (Vite/React)
    │  POST /api/chat (SSE)
    ▼
Spring Boot
    ├── CAG digest builder (from DB, short TTL cache)
    ├── Spring AI chat + tools → OpenRouter
    └── JPA repos → Postgres (products, exclusions, FAQs)
```

## Grounding strategy (CAG + tools; RAG later)

### CAG (context augmentation)

On each chat turn (or from a short TTL cache), inject a compact product digest into the system prompt:

- Product names, taglines, “from” premiums, headline cover limits
- Hard rules: product info only; not legal/financial advice; personal account questions → refuse and point to registration stub

**Why CAG when we already have tools?** Tools answer *specific* lookups; CAG gives the model a default map of the catalog so it can:

1. **Route correctly** — know Income Protection / Health Cash / Device Protection exist before calling tools (fewer wrong or missed tool calls).
2. **Answer cheap/fast questions** without a round trip — “what products do you offer?”, high-level comparisons.
3. **Enforce behavior** — refuse personal-policy questions, stay on demo products, don’t invent cover outside the digest.
4. **Stay coherent** across a multi-turn chat when the user says “the second one” or switches products.

Tools remain the source of truth for limits, exclusions, and FAQs. CAG is the small always-on briefing; tools are the deep dive. If the digest ever duplicates tool data and drifts, shrink the digest to names + rules only.

### Tools (live company DB)

| Tool | Purpose |
|------|---------|
| `listProducts` | Catalog overview |
| `getProduct` | Details for one product by slug/id |
| `getExclusions` | Exclusions for a product |
| `getFaqs` | FAQ rows for a product |

Prefer tool results over model memory for prices, limits, and exclusions. If tools return empty, say the catalog does not have that — do not invent cover.

### RAG (deferred)

Add when policy wordings / playbooks outgrow the digest or when citation of specific document chunks is required. Schema can later index `product_faqs` and document blobs without changing the chat API surface.

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

### Seed products

1. **Income Protection** — income replacement, demo limits/premiums  
2. **Health Cash** — hospital/emergency cash benefits  
3. **Device Protection** — phone/laptop/tablet cover  

Numbers may be Casava-shaped demo values; labeled as demo data.

## Backend components

- **`product`** — entities, repositories, seed runner, optional public catalog REST
- **`ai`** — chat controller (SSE), chat service, system prompt / CAG builder, tool callbacks, session/history store (in-memory for demo)
- **`config`** — CORS for Vite origin, OpenRouter key/model, datasource

### Chat API (sketch)

- `POST /api/chat`  
  - Body: `{ "sessionId": string, "message": string }` (server keeps short per-session history)  
  - Response: SSE stream of events, e.g. `token`, `status` (optional “looking up…”), `done`, `error`

### Caps

- Max history messages retained per session: **20**
- Max tool-call rounds per turn: **5**
- Upstream timeout: **60s** → SSE `error` event
- CAG digest cache TTL: **60s**

## Frontend components

- Brand + one-line value prop + primary CTA into chat  
- Chat panel: message list, streaming assistant bubble, suggested prompts  
- Stub nav: Get a quote / File a claim → “Register to continue” (no auth)  
- Friendly handling of SSE errors and missing API key / backend down

## Error handling

| Case | Behavior |
|------|----------|
| Missing OpenRouter key | HTTP 503 / SSE error; UI friendly message |
| Rate limit / timeout | SSE error; UI “Try again” |
| Tool failure | Structured error to model; admit lookup failed |
| Personal policy question | Refusal via system rules (no personal tools exist) |

No stack traces or secrets in client responses. No intentional PII collection beyond free-text chat.

## Testing

- **Unit:** CAG digest builder; tool → repository mapping; refusal wording for personal questions  
- **Integration:** Chat endpoint with mocked OpenRouter client; streaming + tool path  
- **Manual script:** limits, exclusions, compare products, “what’s on my policy?”, unknown product

## Future extensions (not v1)

- Auth + persona/session binding to real policies  
- Tool calls for policy status and claims  
- RAG over wordings and claims playbooks  
- Quote calculator and claims file stubs becoming real flows  
- Optional Next.js marketing surface if needed later

## Decisions log

| Topic | Decision |
|-------|----------|
| Demo focus | Ask Casa first; stub quote/claims |
| Knowledge source | Company DB (product catalog) |
| Personal data | Out of scope until registration/auth |
| Grounding | Hybrid CAG + tools; RAG later |
| UI | Chat-first shell |
| Frontend | Vite + React + TypeScript |
| Backend | Spring Boot 4.1.1 + Spring AI + OpenRouter |
| Streaming | Yes (SSE) |
| Products | Income Protection, Health Cash, Device Protection |
| Overall approach | Monolith Spring + thin React |
