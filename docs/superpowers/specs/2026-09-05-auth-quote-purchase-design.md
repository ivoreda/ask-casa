# Ask Casa — Auth, Quote & Purchase Design

**Date:** 2026-09-05  
**Status:** Approved for implementation planning  
**Extends:** `docs/superpowers/specs/2026-09-03-casava-demo-ai-design.md`  
**Repo worktree:** `.worktrees/ask-casa-rag` (`feature/ask-casa-rag` → `origin/main`)

## Goal

Add **email/password auth (JWT)**, a **shared quote engine** (form + Ask Casa tools), and a **demo purchase path** that creates user-owned policies — without real payment providers or email verification.

## Success criteria

- Visitor can register, log in, log out; `GET /api/auth/me` returns the current user with a Bearer JWT.
- Logged-in user can get a quote for Income Protection, Health Cash, or Device Protection via UI and via Ask Casa tool.
- Premium comes from a deterministic formula based on product `monthlyFrom` + inputs (demo pricing, clearly labeled).
- Logged-in user can complete demo checkout and receive an `ACTIVE` policy; `GET /api/policies/me` lists their policies.
- Unauthenticated chat still answers product RAG questions; quote/policy actions require login.
- File a claim remains a stub.
- No real card charging, OAuth, magic links, or password-reset email.

## Non-goals

- Real payment gateway (Stripe/Paystack)
- Email verification or password reset
- Cookie sessions / CSRF
- Claims filing
- Underwriting beyond the demo formula

## Decisions

| Topic | Choice |
|-------|--------|
| Quote + Casa | Shared engine; form CTA + chat tools |
| Pricing | Simple formula from `monthlyFrom` + inputs |
| Purchase | Demo pay → policy in H2 |
| Auth | Email + password, Spring Security |
| Auth scope | Register, login, logout, protected routes |
| Session | JWT in memory + `sessionStorage` for demo reloads |
| Claims | Still stubbed |

## Architecture

```
Frontend (Vite)
  Auth screens (register / login)
  Quote form → Checkout → My policies
  Ask Casa (Bearer token when logged in)
        │
        ▼
Spring Boot + Spring Security (JWT filter)
  /api/auth/*          public register/login; me authenticated
  /api/quotes/*        authenticated
  /api/purchases       authenticated
  /api/policies/*      authenticated (owner only)
  /api/chat            public RAG; tools gated by auth
        │
        ▼
H2: users, quotes, policies + existing products / knowledge index
```

## Auth

### Data

`users`: `id` (UUID), `email` (unique), `passwordHash`, `name`, `createdAt`

### API

| Method | Path | Auth | Body / result |
|--------|------|------|----------------|
| POST | `/api/auth/register` | public | `{ email, password, name }` → `{ token, user }` |
| POST | `/api/auth/login` | public | `{ email, password }` → `{ token, user }` |
| GET | `/api/auth/me` | Bearer | `{ id, email, name }` |

### Security rules

- Passwords: BCrypt  
- JWT signed with `JWT_SECRET` (env; required in prod/Railway)  
- Public: register, login, product RAG chat, static health if any  
- Authenticated: quotes, purchases, policies, chat tools that mutate/read account data  
- CORS continues via `CASAVA_AI_FRONTEND_ORIGIN`

### Frontend auth UX

- Nav: Register / Log in when anonymous; after auth: Get a quote, My policies, Log out  
- Store JWT in memory and `sessionStorage` (demo convenience)  
- Send `Authorization: Bearer <token>` on API and chat requests when present  
- Hitting Get a quote while logged out → prompt to register/login  

## Quote engine

### Inputs by product

| Product slug | Inputs | Cover (demo) |
|--------------|--------|--------------|
| `income-protection` | `monthlyIncome`, `coverMonths` (3–12) | `monthlyIncome × coverMonths` |
| `health-cash` | `dependants` (0–4) | base limit × `(1 + 0.25 × dependants)` |
| `device-protection` | `deviceValue` | `min(deviceValue, product max)` |

### Premium formula

`monthlyPremium = round(monthlyFrom × factor)` where `factor` scales with how far requested cover sits above a reference band, with a sane cap. Constants live in `QuoteService` and are labeled **demo pricing / not binding**.

### Entities

**quotes:** `id`, `userId`, `productSlug`, `inputs` (JSON), `monthlyPremium`, `coverAmount`, `status` (`OPEN` \| `PURCHASED`), `createdAt`

**policies:** `id`, `userId`, `quoteId`, `productSlug`, `holderName`, `holderEmail`, `monthlyPremium`, `coverAmount`, `status` (`ACTIVE`), `policyNumber`, `createdAt`

### Quote / purchase API

| Method | Path | Notes |
|--------|------|--------|
| POST | `/api/quotes` | Body: productSlug + inputs → quote owned by current user |
| GET | `/api/quotes/{id}` | Owner only |
| POST | `/api/purchases` | `{ quoteId }` → demo pay; uses user name/email; marks quote `PURCHASED`; returns policy |
| GET | `/api/policies/me` | Current user’s policies |
| GET | `/api/policies/{id}` | Owner only |

## Ask Casa integration

- **Anonymous:** RAG product Q&A only. If user asks for a quote or “my policy,” instruct them to log in / register.  
- **Authenticated:** tools  
  - `createQuote` → `QuoteService`  
  - `listMyPolicies` / `getPolicy` → owner-scoped policy reads  
- System prompt: demo pricing disclaimer; not legal/financial advice; claim filing not available.  
- Chat request may include Bearer token; tools use `SecurityContext` principal.

## UI flows

1. **Register → Login**  
2. **Get a quote** — product tabs → inputs → premium summary → Continue to buy  
3. **Checkout** — confirm details → **Pay (demo)** → success + policy number  
4. **My policies** — list + detail  
5. **Ask Casa** — quote via chat when logged in; policy questions when user has policies  
6. **File a claim** — unchanged stub modal  

## Errors

| Case | Behavior |
|------|----------|
| Bad credentials / missing JWT | 401 |
| Access another user’s quote/policy | 403/404 |
| Duplicate email | 409 |
| Invalid quote inputs | 400 with field errors |
| Quote already purchased | 409 |
| Chat tool failure | Honest refusal; no invented premiums |

## Testing

- Unit: quote formula per product; purchase transitions quote → policy  
- API: register → login → quote → purchase → list policies; unauthorized access rejected  
- Chat: authenticated tool path uses same QuoteService (LLM mocked as needed)  
- Manual: full UI path + Casa “quote me for device protection” while logged in  

## Config / Railway

| Variable | Purpose |
|----------|---------|
| `JWT_SECRET` | Sign JWTs (required in deployed env) |
| `OPENROUTER_API_KEY` | Existing |
| `CASAVA_AI_FRONTEND_ORIGIN` | CORS |
| `VITE_API_URL` | Frontend build-time API origin |
| `PORT` | Existing |

## Relationship to prior spec

Supersedes v1 non-goals that excluded auth and quote/purchase stubs. RAG knowledge path, H2 + SimpleVectorStore, SSE chat, and three seeded products remain as in the 2026-09-03 design.
