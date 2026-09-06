# Casava · Ask Casa

Demo chat app that answers Casava product questions via RAG (H2 + SimpleVectorStore + OpenRouter), with JWT auth, demo quotes, and demo purchase → policies.

## Prerequisites

- Java 21 (`JAVA_HOME` pointing at JDK 21)
- Node.js 20+
- An [OpenRouter](https://openrouter.ai/) API key (chat + embeddings)

## Run

1. Set your OpenRouter API key:

```bash
export OPENROUTER_API_KEY=your-key-here
# optional overrides:
# export OPENROUTER_CHAT_MODEL=openai/gpt-4o-mini
# export OPENROUTER_EMBEDDING_MODEL=openai/text-embedding-3-small
```

2. Start the Spring Boot backend (from `backend/` so relative `./data` paths resolve):

```bash
cd backend
./mvnw spring-boot:run
```

On first start the app seeds three products (Income Protection, Health Cash, Device Protection) and reindexes them into the vector store. Expect a log line about indexed knowledge chunks.

Data files (gitignored):

- `backend/data/casava-db*` — H2 catalog
- `backend/data/vector-store.json` — embeddings index

3. Start the Vite frontend:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 — the API listens on http://localhost:8080.

Optional: point the UI at a remote API during local/dev builds:

```bash
export VITE_API_URL=https://your-api.up.railway.app
npm run dev
```

`VITE_API_URL` is the **API origin only** (no `/api/chat`). Vite bakes it in at **build** time — on Railway, set it on the frontend service and **redeploy/rebuild** (runtime-only env vars will not change an already-built bundle).

## Auth, quotes & policies

### Register / login

1. **Register** with email, password, and name → receives a JWT and user profile.
2. **Log in** with the same credentials → same token + user response.
3. The UI keeps the Bearer token in memory and `sessionStorage` (demo reloads).
4. **Log out** clears the token. `GET /api/auth/me` returns the current user when authenticated.

Nav when anonymous: Register / Log in / Get a quote. After auth: Get a quote, My policies, My claims, Log out.

### Quote → demo pay → my policies

1. **Get a quote** — works logged out. Pick Income Protection, Health Cash, or Device Protection; enter product inputs; see a monthly premium summary (guest preview is not persisted).
2. **Continue to buy** — requires register/login. After auth, an owned quote is created from the same inputs → confirm holder details → **Pay (demo)** (no real card charge).
3. Checkout creates an `ACTIVE` policy and marks the quote purchased.
4. **My policies** / **My claims** — still require auth; lists data owned by the logged-in user.

File a claim remains auth-gated (stub modal when not logged in).

### Ask Casa

- **Anonymous:** product RAG Q&A plus demo price previews. Buying, policies, and claims get a login nudge.
- **Authenticated:** chat sends `Authorization: Bearer <token>`. Tools can create a demo quote and list/read **your** policies and claims (same services as the UI). Other users’ policy IDs are rejected.

### Demo pricing disclaimer

Premiums use a simple deterministic formula from each product’s `monthlyFrom` plus inputs. **Demo pricing only — not binding, not underwriting, not legal or financial advice.** No real payment provider.

## Railway / env

### Backend (API) service

| Variable | Required | Notes |
|----------|----------|--------|
| `OPENROUTER_API_KEY` | yes | Chat + embeddings |
| `JWT_SECRET` | **yes on Railway** | Signs JWTs; use a long random secret. Local default exists for dev only — do not reuse in prod. |
| `CASAVA_AI_FRONTEND_ORIGIN` | yes (prod) | CORS: browser origin of the web app |
| `DATABASE_URL` | Railway Postgres | Railway Postgres URL (`postgres://...`). When set, API uses Postgres; when unset, local H2. |
| `PORT` | injected | App binds to `${PORT:8080}` |

Attach Railway Postgres to the API service so `DATABASE_URL` is injected. Relational data (users, quotes, policies, claims) then survives redeploys. The vector store JSON may still rebuild on redeploy (ephemeral disk); product seed / reindex runs on startup against whichever DB is active.

Set CORS on the **API** service (runtime), then redeploy:

```bash
CASAVA_AI_FRONTEND_ORIGIN=https://your-frontend.up.railway.app
```

Must match the browser origin exactly (scheme + host, no trailing slash). Comma-separate multiple origins if needed. Startup logs include `CORS allowed origins: [...]`.

Also set:

```bash
JWT_SECRET=your-long-random-secret
OPENROUTER_API_KEY=your-key-here
```

### Frontend (web) service

| Variable | Notes |
|----------|--------|
| `VITE_API_URL` | Build-time API origin (no path). Redeploy/rebuild after changing. |

## Tests

```bash
cd backend
./mvnw test
```

```bash
cd frontend
npm run build
```

## Manual verification script

With backend + frontend running and a real OpenRouter key:

| Step | Expect |
|------|--------|
| Register / login | JWT stored; me works; nav shows quote / policies / claims |
| Guest Get a quote | Premium updates with inputs (demo pricing); no login required |
| Continue to buy (logged out) | Register / login prompt, then owned quote + checkout |
| Demo pay | Policy appears under My policies |
| Chat logged out “quote me” | Demo price preview; buy needs login |
| Chat logged in “quote device…” | Tool quote + numbers from quote engine |
| Chat “my policies” | Lists owned policies |
| What’s covered under Device Protection? | Grounded answer + citation chips |
| What’s on my policy? (logged out) | Refusal + register / account message |
| File a claim / My claims | Auth required |
| Other user’s policy id | 403/404 |

## Stack notes

- No Docker — local H2 (default) or Railway Postgres via `DATABASE_URL` + file-backed SimpleVectorStore
- Spring Boot 4.1.1 + Spring Security (JWT) + Spring AI 2.0.x → OpenRouter
- Vite + React + TypeScript chat shell
