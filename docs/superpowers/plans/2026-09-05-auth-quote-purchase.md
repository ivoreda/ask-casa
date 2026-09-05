# Auth, Quote & Purchase Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add JWT email/password auth, a shared quote engine (UI + Ask Casa tools), and demo checkout that creates user-owned policies.

**Architecture:** Spring Security JWT over existing Spring Boot API; `QuoteService` / `PurchaseService` own pricing and policy creation; Vite stores Bearer token and drives quote → buy → my policies; authenticated chat tools call the same services.

**Tech Stack:** Spring Boot 4.1.1, Spring Security, JJWT (or `spring-security-oauth2-jose`), BCrypt, existing H2/JPA, Vite React TS, existing SSE chat

**Spec:** `docs/superpowers/specs/2026-09-05-auth-quote-purchase-design.md`

**Work from:** `/Users/enyatastaff/dev/casava-demo/.worktrees/ask-casa-rag`

---

## File structure

```
backend/src/main/java/com/casava/demo/
  auth/
    User.java
    UserRepository.java
    AuthDtos.java              # RegisterRequest, LoginRequest, AuthResponse, UserResponse
    JwtService.java
    AuthService.java
    AuthController.java
    JwtAuthenticationFilter.java
    SecurityConfig.java
    CurrentUser.java           # helper to resolve UUID from SecurityContext
  quote/
    Quote.java
    QuoteStatus.java
    QuoteRepository.java
    QuoteRequest.java
    QuoteResponse.java
    QuoteService.java          # formula + create/get
    QuoteController.java
  purchase/
    Policy.java
    PolicyStatus.java
    PolicyRepository.java
    PurchaseRequest.java
    PolicyResponse.java
    PurchaseService.java
    PolicyController.java      # /api/purchases + /api/policies
  ai/
    SystemPrompt.java          # update
    RagChatService.java        # wire tools when authenticated
    QuoteTools.java            # @Tool createQuote, listMyPolicies, getPolicy
frontend/src/
  auth/
    AuthContext.tsx
    api.ts                     # register/login/me with token
    LoginForm.tsx
    RegisterForm.tsx
  quote/
    QuoteModal.tsx
    CheckoutPanel.tsx
    MyPolicies.tsx
    api.ts
  api/chat.ts                  # send Authorization when token present
  App.tsx                      # nav + auth gates
```

---

### Task 1: Auth domain + JWT service (TDD)

**Files:**
- Create: `backend/.../auth/User.java`, `UserRepository.java`, `JwtService.java`, `AuthService.java`, `AuthDtos.java`
- Modify: `backend/pom.xml` — add `spring-boot-starter-security`, JJWT deps (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- Modify: `application.yml` — `casava.jwt.secret`, `casava.jwt.expiration-ms`
- Test: `backend/.../auth/JwtServiceTest.java`, `AuthServiceTest.java`

- [ ] **Step 1: Add Maven dependencies**

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.6</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Write failing `JwtServiceTest`**

```java
@Test
void roundTripsUserIdAndEmail() {
  JwtService jwt = new JwtService("test-secret-must-be-long-enough-123456", 3_600_000L);
  String token = jwt.createToken(userId, "ada@example.com");
  assertThat(jwt.parseUserId(token)).isEqualTo(userId);
  assertThat(jwt.parseEmail(token)).isEqualTo("ada@example.com");
}
```

- [ ] **Step 3: Implement `User` entity + `UserRepository` + `JwtService`** — make test pass.

`User`: UUID id, unique email, passwordHash, name, Instant createdAt.

`JwtService` constructor takes `@Value("${casava.jwt.secret}")` and expiration; use HMAC key from secret bytes.

- [ ] **Step 4: Write failing `AuthServiceTest`** (mock repo)

- register hashes password and returns token  
- register duplicate email throws conflict  
- login bad password throws unauthorized  

- [ ] **Step 5: Implement `AuthService` + DTOs** — tests pass.

- [ ] **Step 6: Add yaml**

```yaml
casava:
  jwt:
    secret: ${JWT_SECRET:dev-only-change-me-to-a-long-secret-key}
    expiration-ms: 86400000
```

- [ ] **Step 7: Commit**

```bash
git commit -m "feat: add user entity and JWT auth service"
```

---

### Task 2: Spring Security filter + AuthController

**Files:**
- Create: `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `AuthController.java`, `CurrentUser.java`
- Test: `AuthControllerTest.java` (`@SpringBootTest` + MockMvc or `@WebMvcTest` with security)

- [ ] **Step 1: Write failing API test** — `POST /api/auth/register` then `GET /api/auth/me` with Bearer returns email.

- [ ] **Step 2: Implement `JwtAuthenticationFilter`** — read `Authorization: Bearer`, set `UsernamePasswordAuthenticationToken` with userId principal.

- [ ] **Step 3: Implement `SecurityConfig`**

```java
http.csrf(csrf -> csrf.disable())
  .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
  .authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
    .requestMatchers(HttpMethod.POST, "/api/chat").permitAll()
    .requestMatchers("/api/quotes/**", "/api/purchases", "/api/policies/**", "/api/auth/me").authenticated()
    .anyRequest().permitAll())
  .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

Disable default form login. Expose `PasswordEncoder` + `AuthenticationManager` beans as needed. Ensure CORS from existing `CorsConfig` still works (`http.cors(Customizer.withDefaults())`).

**Important:** Existing `@WebMvcTest` chat tests may need `@Import` / mock security or `@AutoConfigureMockMvc(addFilters = false)` — update so CI stays green.

- [ ] **Step 4: Implement `AuthController`** — register, login, me.

- [ ] **Step 5: Run tests**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd backend && ./mvnw test
```

- [ ] **Step 6: Commit**

```bash
git commit -m "feat: expose JWT auth endpoints with Spring Security"
```

---

### Task 3: Quote formula service (TDD)

**Files:**
- Create: `quote/QuoteService.java` (pricing pure logic can be package-private methods), `QuoteRequest.java` record
- Test: `QuoteServiceTest.java` — no Spring context required for formula unit tests if you extract `QuotePricer`

- [ ] **Step 1: Write failing tests for three products**

```java
@Test
void pricesIncomeProtection() {
  // monthlyIncome=150000, coverMonths=6, monthlyFrom=500
  // coverAmount = 900000
  // factor and premium assert exact expected values you define in implementation
}
```

Define concrete constants in the plan implementation:

```text
REFERENCE_COVER by product:
  income-protection: 500_000
  health-cash: 500_000 (base before dependants)
  device-protection: 500_000

factor = clamp(1.0 + (coverAmount - reference) / reference, 1.0, 3.0)
premium = round(monthlyFrom * factor)   // BigDecimal scale 0 HALF_UP
```

Health: `coverAmount = 500_000 * (1 + 0.25 * dependants)`.  
Device: `coverAmount = min(deviceValue, 1_500_000)`.  
Income: `coverAmount = monthlyIncome * coverMonths`.

Validate ranges; throw `IllegalArgumentException` / custom `BadRequestException` mapped to 400.

- [ ] **Step 2: Implement pricer + tests pass**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat: add deterministic demo quote pricing"
```

---

### Task 4: Quote & policy persistence + REST

**Files:**
- Create: quote/policy entities, repos, `QuoteService.create`, `PurchaseService`, controllers
- Test: `QuotePurchaseIntegrationTest.java` (`@SpringBootTest` + MockMvc + JWT)

- [ ] **Step 1: Entities**

`Quote`: UUID id, UUID userId, String productSlug, JSON inputs map, BigDecimal monthlyPremium, BigDecimal coverAmount, QuoteStatus status, Instant createdAt  

`Policy`: UUID id, UUID userId, UUID quoteId, String productSlug, holderName, holderEmail, premiums/cover, PolicyStatus ACTIVE, String policyNumber (`CSV-` + short id), Instant createdAt  

- [ ] **Step 2: Failing integration test**

Register → login → POST quote device → POST purchase → GET `/api/policies/me` size 1; second user cannot GET that policy.

- [ ] **Step 3: Implement services/controllers**

`QuoteService.create(userId, request)` loads `Product` by slug, prices, saves OPEN quote.  
`PurchaseService.purchase(userId, quoteId)` verifies ownership + OPEN, sets PURCHASED, creates policy with user name/email from `User`.

- [ ] **Step 4: Tests green; commit**

```bash
git commit -m "feat: add quote and demo purchase APIs"
```

---

### Task 5: Frontend auth

**Files:**
- Create: `frontend/src/auth/*`
- Modify: `App.tsx`, `api/chat.ts` (optional header later in Task 7)
- Use existing `VITE_API_URL` base

- [ ] **Step 1: `AuthContext`** — token/user state; hydrate from `sessionStorage`; `login`, `register`, `logout`, `authHeaders()`

- [ ] **Step 2: Login + Register forms** — call `/api/auth/*`, store token

- [ ] **Step 3: Wire nav** — show auth vs logged-in links

- [ ] **Step 4: `npm run build`** passes

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: add register and login UI with JWT storage"
```

---

### Task 6: Frontend quote, checkout, my policies

**Files:**
- Create: `frontend/src/quote/*`
- Modify: `App.tsx` — replace Get-a-quote stub with `QuoteModal`; keep File-a-claim stub

- [ ] **Step 1: Quote API client** — POST `/api/quotes`, POST `/api/purchases`, GET `/api/policies/me` with Bearer

- [ ] **Step 2: `QuoteModal`** — product tabs + inputs + submit → show premium + “Continue to buy” (if logged out, open auth first)

- [ ] **Step 3: `CheckoutPanel`** — confirm → Pay (demo) → show policy number

- [ ] **Step 4: `MyPolicies`** — list policies

- [ ] **Step 5: Build + commit**

```bash
git commit -m "feat: add quote checkout and my policies UI"
```

---

### Task 7: Ask Casa tools + prompt + chat auth header

**Files:**
- Modify: `SystemPrompt.java`, `RagChatService.java`, `OpenAiChatTokenStreamer.java` (or ChatClient bean) to enable tools
- Create: `ai/QuoteTools.java` (or `AccountTools.java`)
- Modify: `ChatController` / frontend `chat.ts` to forward `Authorization`
- Test: unit test that tools delegate to QuoteService/PolicyRepository with fixed user

- [ ] **Step 1: Update system prompt** — login required for quotes/policies; demo pricing; tools available when authenticated

- [ ] **Step 2: Implement tools**

```java
@Tool(description = "Create a demo insurance quote for the logged-in user")
public String createQuote(String productSlug, ...) { ... }
```

Only register tools with ChatClient when `CurrentUser.isAuthenticated()`; otherwise RAG-only path (current behavior).

- [ ] **Step 3: Frontend `streamChat`** — accept optional token, set `Authorization` header

- [ ] **Step 4: Manual / test path** — authenticated createQuote returns premium string containing numbers from QuoteService

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: enable Ask Casa quote and policy tools for logged-in users"
```

---

### Task 8: Docs, Railway env, full verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Document** `JWT_SECRET`, auth flows, quote demo disclaimer

- [ ] **Step 2: Run**

```bash
cd backend && ./mvnw test
cd frontend && npm run build
```

- [ ] **Step 3: Manual checklist**

| Step | Expect |
|------|--------|
| Register / login | JWT + me works |
| Quote each product | Premium updates with inputs |
| Demo pay | Policy in My policies |
| Chat logged out “quote me” | Asks to log in |
| Chat logged in “quote device…” | Tool quote + numbers |
| Chat “my policies” | Lists owned policies |
| File a claim | Stub unchanged |
| Other user’s policy id | 403/404 |

- [ ] **Step 4: Commit + push main**

```bash
git commit -m "docs: document auth and quote purchase flows"
git push origin HEAD:main
```

---

## Spec coverage

| Spec item | Task |
|-----------|------|
| Register/login/me JWT | 1–2, 5 |
| Quote formula + API | 3–4, 6 |
| Demo purchase + policies | 4, 6 |
| Casa tools when authed | 7 |
| Anonymous RAG + login nudge | 7 |
| File claim stub | 6 (unchanged) |
| Railway JWT_SECRET | 8 |

---

## Execution handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-05-auth-quote-purchase.md`.

**1. Subagent-Driven (recommended)** — fresh subagent per task  
**2. Inline Execution** — this session with checkpoints  

Which approach?
