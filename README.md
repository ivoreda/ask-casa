# Casava · Ask Casa

Demo chat app that answers Casava product questions via RAG (H2 + SimpleVectorStore + OpenRouter).

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

| Prompt | Expect |
|--------|--------|
| What’s covered under Device Protection? | Grounded answer + citation chips |
| What are exclusions for Health Cash? | Exclusion-oriented answer + citations |
| Compare Income Protection and Health Cash | Uses multiple product sources |
| What’s on my policy? | Refusal + register / account message |
| Do you cover spaceships? | Admits knowledge base has no match / no invented cover |
| Get a quote / File a claim | “Register to continue” modal |

## Stack notes

- No Docker — local H2 + file-backed SimpleVectorStore
- Spring Boot 4.1.1 + Spring AI 2.0.x → OpenRouter
- Vite + React + TypeScript chat shell
