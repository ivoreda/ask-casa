# Casava · Ask Casa

Demo chat app that answers Casava product questions via RAG (H2 + SimpleVectorStore + OpenRouter).

## Prerequisites

- Java 21
- Node.js 20+
- An [OpenRouter](https://openrouter.ai/) API key

## Run

1. Set your OpenRouter API key:

```bash
export OPENROUTER_API_KEY=your-key-here
```

2. Start the Spring Boot backend:

```bash
cd backend
./mvnw spring-boot:run
```

The app uses a file-based H2 database under `backend/data/casava-db` and persists the vector index to `backend/data/vector-store.json`.

3. Start the Vite frontend:

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 — the API listens on http://localhost:8080.
