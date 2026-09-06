# Ask Casa — Postgres persistence & guest quotes Design

**Date:** 2026-09-06  
**Status:** Approved for implementation planning  
**Extends:** `docs/superpowers/specs/2026-09-05-auth-quote-purchase-design.md`, `docs/superpowers/specs/2026-09-06-claims-list-visibility-design.md`  
**Repo worktree:** `.worktrees/ask-casa-rag` (`feature/ask-casa-rag`)

## Goal

1. Persist users, quotes, policies, and claims across Railway restarts/redeploys using **Postgres** in production while keeping **H2** for local development.
2. Allow **guests to get a demo quote without registering**; require login to **buy**, **file claims**, and view **My policies / My claims**.

## Success criteria

- With Railway Postgres configured, register → buy a policy → redeploy API → user and policy still exist; same email cannot register again (409).
- Local default (no `DATABASE_URL`) still boots on H2 as today.
- Unauthenticated user can open **Get a quote**, enter inputs, and see demo premium via public preview API.
- **Continue to buy** while logged out prompts register/login; after auth, a **new owned quote** is created from the same inputs, then checkout proceeds.
- Unauthenticated `POST /api/quotes` (persist), purchases, policies, claims remain 401.
- Ask Casa can give demo prices when logged out (preview); persisted `createQuote` when logged in; buy/claims/policies still require login messaging when tools are absent.

## Non-goals

- Guest claim filing or claim-by-policy-number without login
- Saving guest quotes with nullable `userId` / attaching after login
- Flyway/Liquibase versioned migrations (demo keeps `ddl-auto=update`)
- Moving SimpleVectorStore off the filesystem (vector JSON may still reset on redeploy; product catalog in DB is re-seeded)
- Real payment providers or email verification

## Decisions

| Topic | Choice |
|-------|--------|
| Auth for quote | Guest OK (preview only) |
| Auth for buy / claims / my policies / my claims | Login required |
| Guest quote lifecycle | Ephemeral preview; buy recreates owned quote after login |
| Prod DB | Railway Postgres via `DATABASE_URL` (or Spring datasource URL) |
| Local DB | H2 file (current default) when no Postgres URL |
| Schema | Hibernate `ddl-auto=update` |
| Docker | Not required for local app or local DB |

## Architecture

```
Local (default)
  Spring Boot → H2 file ./data/casava-db
  SimpleVectorStore → ./data/vector-store.json

Prod (Railway)
  Spring Boot → Postgres (Railway plugin / DATABASE_URL)
  SimpleVectorStore → ephemeral disk (RAG only; may rebuild on reindex)

Guest quote
  UI / Casa → POST /api/quotes/preview → QuotePricer (no row)

Buy path
  Auth → POST /api/quotes (persist) → POST /api/purchases → policy
```

## Postgres configuration

- Add PostgreSQL JDBC driver dependency.
- When a Postgres URL is present, configure datasource to Postgres; otherwise keep H2 settings in `application.yml`.
- Support Railway-style `DATABASE_URL` (`postgres://user:pass@host:port/db`) by converting to JDBC `jdbc:postgresql://...` if Spring Boot does not auto-bind it in this setup.
- Env documentation for API service:
  - `DATABASE_URL` (or explicit `SPRING_DATASOURCE_*`) from Railway Postgres
  - Existing `JWT_SECRET`, `OPENROUTER_API_KEY`, `CASAVA_AI_FRONTEND_ORIGIN`
- No H2 volume required in prod once Postgres is attached.
- Startup seed (`ProductSeedRunner`) and optional knowledge reindex continue to run against whichever DB is active.

## Quote APIs

| Method | Path | Auth | Behavior |
|--------|------|------|----------|
| POST | `/api/quotes/preview` | public | Same request body as create; returns `productSlug`, inputs, `monthlyPremium`, `coverAmount`, demo disclaimer fields as needed; **does not** persist; **no** durable quote id |
| POST | `/api/quotes` | Bearer | Unchanged: create OPEN quote owned by current user |
| GET | `/api/quotes/{id}` | Bearer | Owner only (unchanged) |
| POST | `/api/purchases` | Bearer | Unchanged |
| GET | `/api/policies/**`, `/api/claims/**` | Bearer | Unchanged |

Shared pricing: preview and create both use existing `QuoteService.price` / `QuotePricer` + product `monthlyFrom`.

### SecurityConfig

- `POST /api/quotes/preview` → `permitAll`
- Other `/api/quotes/**`, `/api/purchases`, `/api/policies/**`, `/api/claims/**`, `/api/auth/me` → `authenticated`
- Register/login and `POST /api/chat` remain public

## Frontend

- **Get a quote** does **not** require login to open.
- Guest: form → `preview` → show premium → **Continue to buy** → auth modal → after success, `POST /api/quotes` with same inputs → checkout.
- Logged-in: form → `POST /api/quotes` → checkout (current happy path).
- File a claim / My policies / My claims: still auth-gated.

## Ask Casa

- When **not** authenticated: expose a preview-oriented tool (or adjust `createQuote` to preview-only without persist) so price questions work; instruct user to log in to buy.
- When **authenticated**: keep persisting `createQuote`.
- System / auth prompts: distinguish product RAG, guest pricing, and account actions (buy, policies, claims).

## Errors

| Case | Behavior |
|------|----------|
| Invalid preview/create inputs | 400 |
| Persist quote / purchase / claims / policies without JWT | 401 |
| Duplicate email | 409 |
| Access another user’s quote/policy | 403/404 |

## Testing

- Unit: preview premium equals create pricing for same inputs.
- API: preview without JWT → 200; create without JWT → 401; register → create → purchase → list policies.
- Frontend build; manual guest quote → login → buy.
- Railway smoke: data survives API redeploy when Postgres is attached.

## Relationship to prior specs

- Supersedes “quote requires login” from the 2026-09-05 auth-quote-purchase design for **pricing only**; purchase and account surfaces remain authenticated.
- Claims list/visibility rules unchanged (still login).
- Does not change RAG grounding model; only where relational data lives in prod.
