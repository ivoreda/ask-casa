# Claims List & Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let logged-in users see filed demo claims in a **My claims** UI and via Ask Casa (`listMyClaims`), including claims when summarizing policies.

**Architecture:** Enrich existing `GET /api/claims/me` / `POST /api/claims` responses with `policyNumber` + `productSlug`. Add `listMyClaims` AccountTool and prompt rules. Frontend adds a My claims modal (My policies pattern) and refreshes after filing.

**Tech Stack:** Spring Boot 4.1.1, existing claims/purchase/auth packages, Vite React TS

**Spec:** `docs/superpowers/specs/2026-09-06-claims-list-visibility-design.md`

**Work from:** `/Users/enyatastaff/dev/casava-demo/.worktrees/ask-casa-rag`

**Java:** `export JAVA_HOME=/opt/homebrew/opt/openjdk@21` before `./mvnw`

---

## File structure

```
backend/src/main/java/com/casava/demo/
  claims/
    ClaimResponse.java          # add policyNumber, productSlug; from(claim, …)
    ClaimService.java           # toResponse / listMineResponses; enrich after file
    ClaimController.java        # use enriched service methods
  ai/
    AccountTools.java           # listMyClaims
    SystemPrompt.java           # listMyClaims + policy+claims guidance

backend/src/test/java/com/casava/demo/
  claims/ClaimServiceTest.java  # enrichment cases
  ai/AccountToolsTest.java      # listMyClaims
  ai/SystemPromptTest.java      # prompt mentions

frontend/src/
  claims/
    api.ts                      # ClaimResponse fields + listMyClaims()
    MyClaims.tsx                # new modal
    FileClaimModal.tsx          # onFiled callback
  App.tsx                       # My claims nav + claimsRefresh
```

---

### Task 1: Enrich ClaimResponse in ClaimService (TDD)

**Files:**
- Modify: `backend/src/main/java/com/casava/demo/claims/ClaimResponse.java`
- Modify: `backend/src/main/java/com/casava/demo/claims/ClaimService.java`
- Modify: `backend/src/main/java/com/casava/demo/claims/ClaimController.java`
- Test: `backend/src/test/java/com/casava/demo/claims/ClaimServiceTest.java`

- [ ] **Step 1: Extend the failing enrichment test**

In `ClaimServiceTest.java`, add:

```java
@Test
void listMineResponsesIncludesPolicyNumberAndProductSlug() {
  UUID claimId = UUID.randomUUID();
  Claim claim = new Claim();
  claim.setId(claimId);
  claim.setUserId(userId);
  claim.setPolicyId(policyId);
  claim.setDescription("Phone stolen from bag while commuting to work.");
  claim.setStatus(ClaimStatus.SUBMITTED);
  claim.setClaimNumber("CLM-TEST0001");
  claim.setCreatedAt(Instant.parse("2026-09-06T06:00:00Z"));

  Policy policy = new Policy();
  policy.setId(policyId);
  policy.setPolicyNumber("POL-ABC12345");
  policy.setProductSlug("device-protection");

  when(claimRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(claim));
  when(purchaseService.getOwned(userId, policyId)).thenReturn(policy);

  List<ClaimResponse> responses = claimService.listMineResponses(userId);

  assertThat(responses).hasSize(1);
  ClaimResponse r = responses.getFirst();
  assertThat(r.claimNumber()).isEqualTo("CLM-TEST0001");
  assertThat(r.policyId()).isEqualTo(policyId);
  assertThat(r.policyNumber()).isEqualTo("POL-ABC12345");
  assertThat(r.productSlug()).isEqualTo("device-protection");
}

@Test
void listMineResponsesSurvivesMissingPolicy() {
  Claim claim = new Claim();
  claim.setId(UUID.randomUUID());
  claim.setUserId(userId);
  claim.setPolicyId(policyId);
  claim.setDescription("Phone stolen from bag while commuting to work.");
  claim.setStatus(ClaimStatus.SUBMITTED);
  claim.setClaimNumber("CLM-ORPHAN01");
  claim.setCreatedAt(Instant.now());

  when(claimRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(claim));
  when(purchaseService.getOwned(userId, policyId))
      .thenThrow(new AuthException(HttpStatus.NOT_FOUND, "Policy not found"));

  List<ClaimResponse> responses = claimService.listMineResponses(userId);

  assertThat(responses).hasSize(1);
  assertThat(responses.getFirst().policyNumber()).isNull();
  assertThat(responses.getFirst().productSlug()).isNull();
}
```

Add imports for `ClaimResponse`, `AuthException`, `HttpStatus`, `Instant` as needed. Keep existing `filesClaimAgainstOwnedPolicy` tests.

- [ ] **Step 2: Run tests — expect compile/fail on missing API**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd backend && ./mvnw -q -Dtest=ClaimServiceTest test
```

Expected: FAIL (missing `listMineResponses` and/or `policyNumber` on record).

- [ ] **Step 3: Update `ClaimResponse`**

Replace the record with:

```java
package com.casava.demo.claims;

import java.time.Instant;
import java.util.UUID;

public record ClaimResponse(
    UUID id,
    UUID policyId,
    String description,
    String status,
    String claimNumber,
    Instant createdAt,
    String policyNumber,
    String productSlug) {

  public static ClaimResponse from(Claim claim, String policyNumber, String productSlug) {
    return new ClaimResponse(
        claim.getId(),
        claim.getPolicyId(),
        claim.getDescription(),
        claim.getStatus().name(),
        claim.getClaimNumber(),
        claim.getCreatedAt(),
        policyNumber,
        productSlug);
  }
}
```

- [ ] **Step 4: Add enrichment helpers on `ClaimService`**

Add:

```java
@Transactional(readOnly = true)
public List<ClaimResponse> listMineResponses(UUID userId) {
  return listMine(userId).stream().map(c -> toResponse(userId, c)).toList();
}

public ClaimResponse toResponse(UUID userId, Claim claim) {
  try {
    Policy policy = purchaseService.getOwned(userId, claim.getPolicyId());
    return ClaimResponse.from(claim, policy.getPolicyNumber(), policy.getProductSlug());
  } catch (RuntimeException ex) {
    return ClaimResponse.from(claim, null, null);
  }
}
```

Change `file` to return `ClaimResponse` **or** keep returning `Claim` and map in the controller — prefer keeping `file` returning `Claim` and mapping in controller via `toResponse` so existing `ClaimServiceTest.filesClaimAgainstOwnedPolicy` stays valid.

- [ ] **Step 5: Update `ClaimController`**

```java
@PostMapping("/api/claims")
public ClaimResponse file(@RequestBody ClaimRequest request) {
  UUID userId = CurrentUser.requireUserId();
  Claim claim = claimService.file(userId, request.policyId(), request.description());
  return claimService.toResponse(userId, claim);
}

@GetMapping("/api/claims/me")
public List<ClaimResponse> mine() {
  UUID userId = CurrentUser.requireUserId();
  return claimService.listMineResponses(userId);
}
```

- [ ] **Step 6: Re-run ClaimServiceTest**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd backend && ./mvnw -q -Dtest=ClaimServiceTest test
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/casava/demo/claims/ClaimResponse.java \
  backend/src/main/java/com/casava/demo/claims/ClaimService.java \
  backend/src/main/java/com/casava/demo/claims/ClaimController.java \
  backend/src/test/java/com/casava/demo/claims/ClaimServiceTest.java
git commit -m "$(cat <<'EOF'
feat: enrich claim API responses with policy context

EOF
)"
```

---

### Task 2: `listMyClaims` tool + prompt updates (TDD)

**Files:**
- Modify: `backend/src/main/java/com/casava/demo/ai/AccountTools.java`
- Modify: `backend/src/main/java/com/casava/demo/ai/SystemPrompt.java`
- Test: `backend/src/test/java/com/casava/demo/ai/AccountToolsTest.java`
- Test: `backend/src/test/java/com/casava/demo/ai/SystemPromptTest.java`

- [ ] **Step 1: Failing AccountTools test**

Add to `AccountToolsTest.java`:

```java
@Test
void listMyClaimsDelegatesToClaimService() {
  UUID policyId = UUID.randomUUID();
  Claim claim = new Claim();
  claim.setId(UUID.randomUUID());
  claim.setPolicyId(policyId);
  claim.setClaimNumber("CLM-ABCDEF12");
  claim.setStatus(ClaimStatus.SUBMITTED);
  claim.setDescription("Phone stolen on the bus.");

  when(claimService.listMine(userId)).thenReturn(List.of(claim));
  when(claimService.toResponse(eq(userId), eq(claim)))
      .thenReturn(
          new ClaimResponse(
              claim.getId(),
              policyId,
              claim.getDescription(),
              "SUBMITTED",
              "CLM-ABCDEF12",
              Instant.parse("2026-09-06T06:00:00Z"),
              "POL-XYZ",
              "device-protection"));

  String result = tools.listMyClaims();

  verify(claimService).listMine(userId);
  assertThat(result).contains("CLM-ABCDEF12");
  assertThat(result).contains("POL-XYZ");
  assertThat(result).contains("device-protection");
}
```

Alternatively, if `listMyClaims` calls `listMineResponses` only:

```java
when(claimService.listMineResponses(userId))
    .thenReturn(List.of(/* ClaimResponse ... */));
String result = tools.listMyClaims();
assertThat(result).contains("CLM-ABCDEF12");
```

Prefer **`listMineResponses`** so the tool reuses enrichment.

- [ ] **Step 2: Run test — expect fail**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd backend && ./mvnw -q -Dtest=AccountToolsTest#listMyClaimsDelegatesToClaimService test
```

Expected: FAIL (method missing).

- [ ] **Step 3: Implement `listMyClaims` on `AccountTools`**

```java
@Tool(
    description =
        "List the logged-in user's demo claims (newest first). "
            + "Call this for claim history and when summarizing policies so you can mention claims.")
public String listMyClaims() {
  try {
    UUID userId = CurrentUser.requireUserId();
    List<ClaimResponse> claims = claimService.listMineResponses(userId);
    if (claims.isEmpty()) {
      return "You have no claims yet. Offer to help them file a claim if they have a policy.";
    }
    return claims.stream().map(AccountTools::summarizeClaim).collect(Collectors.joining("\n"));
  } catch (Exception ex) {
    return "Could not list claims: " + message(ex);
  }
}

private static String summarizeClaim(ClaimResponse claim) {
  return "claimNumber="
      + claim.claimNumber()
      + " status="
      + claim.status()
      + " policyId="
      + claim.policyId()
      + " policyNumber="
      + claim.policyNumber()
      + " product="
      + claim.productSlug()
      + " description="
      + claim.description();
}
```

Import `ClaimResponse`.

- [ ] **Step 4: Update `SystemPrompt`**

Replace `TEXT` personal/claims sections and `AUTHENTICATED_USER_PREFIX` so they include:

```java
public static final String TEXT =
    """
    You are Ask Casa, a helpful assistant for this Casava demo insurer.
    This is not legal or financial advice. Quote premiums from tools are demo pricing and not binding.
    Never invent cover, prices, exclusions, premiums, policy details, or claim details.

    Product knowledge:
    - Answer product facts (what products cover, exclusions, FAQs) from the provided Context.
    - If Context does not cover a product fact, say so — do not invent it.

    Personal account (quotes, policies, claims):
    - When account tools are available, you MUST call them for personal questions such as
      "what's on my policy", listing policies, listing claims, creating a quote, or filing a claim.
    - Do NOT say the knowledge base lacks personal policy or claim data without calling the
      relevant tools (listMyPolicies, getPolicy, listMyClaims, createQuote, fileClaim) first.
    - When tools are NOT available, tell the user to log in or register.

    Claims:
    - When fileClaim is available, help the user file a demo claim against one of their policies.
    - Ask for a short incident description if missing. Prefer listing policies first if they have not
      chosen a policy.
    - For claim history ("what claims have I filed?"), call listMyClaims.
    - For policy questions ("what's on my policy?"), call listMyPolicies or getPolicy AND listMyClaims,
      then mention any claims on those policies (or say there are none).
    """;

public static final String AUTHENTICATED_USER_PREFIX =
    """
    The user is logged in. Account tools are available: createQuote, listMyPolicies, getPolicy,
    listMyClaims, fileClaim.
    For questions about their policy/policies, call listMyPolicies (or getPolicy) and listMyClaims
    before answering, and mention related claims.
    For claim history, call listMyClaims.
    For quote requests, call createQuote with the required product inputs.
    For claim filing, call fileClaim after you know policyId and a description.

    """;
```

- [ ] **Step 5: Update `SystemPromptTest`**

```java
@Test
void promptRequiresListMyClaimsForClaimAndPolicyQuestions() {
  String lower = SystemPrompt.TEXT.toLowerCase();
  assertThat(lower).contains("listmyclaims");
  assertThat(lower).contains("claim history");
}

@Test
void authenticatedPrefixMentionsListMyClaims() {
  String lower = SystemPrompt.AUTHENTICATED_USER_PREFIX.toLowerCase();
  assertThat(lower).contains("listmyclaims");
  assertThat(lower).contains("fileclaim");
}
```

Keep existing tests that still match (adjust if wording broke `must call` / `listmypolicies`).

- [ ] **Step 6: Run AI tests**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd backend && ./mvnw -q -Dtest=AccountToolsTest,SystemPromptTest test
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/casava/demo/ai/AccountTools.java \
  backend/src/main/java/com/casava/demo/ai/SystemPrompt.java \
  backend/src/test/java/com/casava/demo/ai/AccountToolsTest.java \
  backend/src/test/java/com/casava/demo/ai/SystemPromptTest.java
git commit -m "$(cat <<'EOF'
feat: add listMyClaims tool and prompt guidance

EOF
)"
```

---

### Task 3: Frontend API + MyClaims modal

**Files:**
- Modify: `frontend/src/claims/api.ts`
- Create: `frontend/src/claims/MyClaims.tsx`
- Modify: `frontend/src/claims/FileClaimModal.tsx`

- [ ] **Step 1: Extend `ClaimResponse` and add `listMyClaims`**

In `frontend/src/claims/api.ts`:

```typescript
export type ClaimResponse = {
  id: string
  policyId: string
  description: string
  status: string
  claimNumber: string
  createdAt: string
  policyNumber: string | null
  productSlug: ProductSlug | null
}

export async function listMyClaims(token: string): Promise<ClaimResponse[]> {
  const res = await fetch(`${API_BASE}/api/claims/me`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error(await parseError(res))
  return (await res.json()) as ClaimResponse[]
}
```

Import `ProductSlug` from `../quote/api` (or type as `string | null` if preferred).

- [ ] **Step 2: Create `MyClaims.tsx`**

Mirror `frontend/src/quote/MyPolicies.tsx`:

```tsx
import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { listMyClaims, PRODUCT_LABELS, type ClaimResponse } from './api'

type MyClaimsProps = {
  open: boolean
  onClose: () => void
  refreshKey?: number
}

export function MyClaims({ open, onClose, refreshKey = 0 }: MyClaimsProps) {
  const { token } = useAuth()
  const [claims, setClaims] = useState<ClaimResponse[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  useEffect(() => {
    if (!open || !token) return
    let cancelled = false
    async function load() {
      setBusy(true)
      setError(null)
      try {
        const list = await listMyClaims(token!)
        if (!cancelled) setClaims(list)
      } catch (err) {
        if (!cancelled) {
          setClaims([])
          setError(err instanceof Error ? err.message : 'Could not load claims')
        }
      } finally {
        if (!cancelled) setBusy(false)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [open, token, refreshKey])

  if (!open) return null

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal quote-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="claims-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="claims-modal-title">My claims</h2>
        <p className="auth-lede">Demo claims filed on this account.</p>

        {busy ? <p className="status-line">Loading claims…</p> : null}
        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}

        {!busy && !error && claims.length === 0 ? (
          <p className="empty-policies">
            No claims yet. Use File a claim when you have an active policy.
          </p>
        ) : null}

        {claims.length > 0 ? (
          <ul className="policy-list">
            {claims.map((claim) => (
              <li key={claim.id} className="policy-item">
                <div className="policy-item-head">
                  <strong>{claim.claimNumber}</strong>
                  <span className="policy-status">{claim.status}</span>
                </div>
                <p className="policy-number">
                  {claim.productSlug
                    ? PRODUCT_LABELS[claim.productSlug]
                    : 'Unknown product'}{' '}
                  · {claim.policyNumber ?? claim.policyId}
                </p>
                <p className="policy-meta">{claim.description}</p>
                <p className="policy-meta">
                  {new Date(claim.createdAt).toLocaleString()}
                </p>
              </li>
            ))}
          </ul>
        ) : null}

        <button type="button" className="btn btn-primary" onClick={onClose}>
          Close
        </button>
      </div>
    </div>
  )
}
```

If `PRODUCT_LABELS[claim.productSlug]` types complain when slug is unexpected, fall back to `claim.productSlug`.

- [ ] **Step 3: Add `onFiled` to `FileClaimModal`**

```tsx
type FileClaimModalProps = {
  open: boolean
  onClose: () => void
  onFiled?: () => void
}
```

After successful `fileClaim`, call `onFiled?.()` (in addition to `setSubmitted(claim)`).

- [ ] **Step 4: Typecheck / build**

```bash
cd frontend && npm run build
```

Expected: PASS (App not wired yet is OK if MyClaims unused — or wire in Task 4 first then build).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/claims/api.ts frontend/src/claims/MyClaims.tsx frontend/src/claims/FileClaimModal.tsx
git commit -m "$(cat <<'EOF'
feat: add My claims modal and list API client

EOF
)"
```

---

### Task 4: Wire App nav + refresh

**Files:**
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Import and state**

```tsx
import { MyClaims } from './claims/MyClaims'

const [claimsOpen, setClaimsOpen] = useState(false)
const [claimsRefresh, setClaimsRefresh] = useState(0)

function onMyClaims() {
  if (!user) {
    openAuth('login')
    return
  }
  setClaimsOpen(true)
}
```

- [ ] **Step 2: Nav button (logged-in block)**

Next to My policies:

```tsx
<button type="button" className="nav-link" onClick={onMyClaims}>
  My claims
</button>
```

- [ ] **Step 3: Modals**

```tsx
<FileClaimModal
  open={claimOpen}
  onClose={() => setClaimOpen(false)}
  onFiled={() => setClaimsRefresh((n) => n + 1)}
/>
<MyClaims
  open={claimsOpen}
  onClose={() => setClaimsOpen(false)}
  refreshKey={claimsRefresh}
/>
```

- [ ] **Step 4: Build**

```bash
cd frontend && npm run build
```

Expected: PASS

- [ ] **Step 5: Full backend test suite**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
cd backend && ./mvnw -q test
```

Expected: PASS (run outside sandbox if Mockito agent fails)

- [ ] **Step 6: Commit**

```bash
git add frontend/src/App.tsx
git commit -m "$(cat <<'EOF'
feat: wire My claims into app navigation

EOF
)"
```

---

### Task 5: Manual verification notes (optional doc touch)

**Files:** none required (manual checklist only)

- [ ] **Step 1: Manual smoke**

1. Log in → own a policy → File a claim  
2. Open **My claims** → see claim number, product, policy number, description  
3. Ask Casa: “what claims have I filed?” → uses tools, lists claims  
4. Ask Casa: “what’s on my policy?” → mentions policy **and** claims (or none)

- [ ] **Step 2: Commit only if you added docs** — skip if no file changes

---

## Spec coverage check

| Spec requirement | Task |
|------------------|------|
| My claims nav + modal | 3, 4 |
| Enriched `policyNumber` / `productSlug` | 1 |
| `listMyClaims` tool | 2 |
| Prompt: claim history + policy+claims | 2 |
| Auth gate for My claims | 4 |
| Refresh after file | 3 (`onFiled`), 4 |
| Tests | 1, 2, 4 |

## Placeholder / consistency check

- `ClaimResponse` fields match frontend type (`policyNumber`, `productSlug` nullable).
- Tool uses `listMineResponses` (same enrichment as REST).
- No nested-under-policies UI (non-goal).
