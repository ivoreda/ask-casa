# Ask Casa — Claims list & visibility Design

**Date:** 2026-09-06  
**Status:** Approved for implementation planning  
**Extends:** `docs/superpowers/specs/2026-09-05-auth-quote-purchase-design.md` (claims filing already shipped beyond that doc’s stub)  
**Repo worktree:** `.worktrees/ask-casa-rag` (`feature/ask-casa-rag`)

## Goal

Let logged-in users **see demo claims they have filed**, and let **Ask Casa** answer claim-history questions and **include claims when summarizing policies** the user paid for.

## Success criteria

- Logged-in user opens **My claims** from the top nav and sees their claims (newest first), including claim number, status, product, policy number, description, and created time.
- Empty state guides them to File a claim when they have none.
- `GET /api/claims/me` returns claims enriched with `policyNumber` and `productSlug`.
- Ask Casa `listMyClaims` tool lists the same data for authenticated chat.
- Asking Casa about claim history uses `listMyClaims`.
- Asking Casa about policies (“what’s on my policy?”) uses policy tools **and** `listMyClaims`, then mentions claims on those policies (or that there are none).
- Unauthenticated users hitting My claims are prompted to log in.

## Non-goals

- Nested claims UI under My policies
- Claim status workflow, adjusters, payouts, document uploads
- New claim endpoints beyond enriching the existing list/create responses
- Changing how claims are filed (modal + `fileClaim` remain)

## Decisions

| Topic | Choice |
|-------|--------|
| UI placement | Separate **My claims** nav item (same pattern as My policies) |
| Casa behavior | Dedicated claim questions **and** include claims in policy summaries |
| Approach | Thin layer on existing `GET /api/claims/me` + new `listMyClaims` tool |
| Policy context on claims | Enrich API/`ClaimResponse` with `policyNumber` + `productSlug` |
| Policy tool shape | Leave `listMyPolicies` / `getPolicy` unchanged; prompt requires calling `listMyClaims` alongside them |

## Architecture

```
Frontend
  My claims modal ──GET /api/claims/me──┐
  File a claim (existing)               │
  Ask Casa (Bearer)                     │
                                        ▼
Spring Boot
  ClaimController (existing routes)
  ClaimService.listMine / file
  AccountTools.listMyClaims (+ existing fileClaim)
  PurchaseService (policy lookup for enrichment)
                                        ▼
H2: claims + policies (owner-scoped)
```

## API

Reuse existing routes; enrich response shape.

| Method | Path | Auth | Notes |
|--------|------|------|--------|
| GET | `/api/claims/me` | Bearer | Newest first; each item includes policy context fields |
| POST | `/api/claims` | Bearer | Unchanged filing rules; response uses same enriched shape |

### `ClaimResponse` (enriched)

| Field | Source |
|-------|--------|
| `id`, `policyId`, `description`, `status`, `claimNumber`, `createdAt` | Claim entity (existing) |
| `policyNumber` | Owned policy for `policyId` |
| `productSlug` | Owned policy for `policyId` |

If the policy row is unexpectedly missing, still return the claim with `policyNumber` / `productSlug` null or empty rather than failing the entire list.

## Ask Casa

### Tool: `listMyClaims`

- Auth required (`CurrentUser.requireUserId()`).
- Returns a line-oriented summary: claim number, status, policy id/number/product, description.
- Empty list → clear “no claims yet” message (optionally nudge File a claim).

### Prompt updates

- Authenticated prefix: include `listMyClaims` in the available tools list.
- System prompt:
  - Claim-history questions → must call `listMyClaims`.
  - Policy questions → call `listMyPolicies` or `getPolicy` **and** `listMyClaims`, then mention related claims (or none).
- Do not invent claim numbers or statuses.

## UI

### My claims

- Nav control visible when logged in (alongside My policies).
- Modal mirrors My policies styling: list, empty state, Close / Escape / backdrop.
- Row content: claim number, status, product label, policy number, description, created date.
- `claimsRefresh` key after successful File a claim so an open My claims modal reloads (same idea as policies after purchase).

### Auth gate

- My claims while logged out → open login modal (same pattern as My policies / File a claim).

## Errors

| Case | Behavior |
|------|----------|
| Missing JWT on `/api/claims/**` | 401 |
| List/file failure in UI | Inline error in modal |
| Tool failure | Tool returns short error string; Casa does not invent claims |

## Testing

- Unit: claim list enrichment maps policy number/slug; `listMyClaims` delegates to `ClaimService`.
- Prompt tests: mention `listMyClaims` and policy+claims guidance.
- Manual: file a claim → My claims shows it; Ask Casa “what claims have I filed?” and “what’s on my policy?” include claim info when present.

## Relationship to prior specs

- Supersedes “File a claim remains a stub” / “Claims filing” non-goals from the 2026-09-05 auth-quote-purchase design (filing already implemented; this spec adds **list/visibility**).
- Does not change RAG product grounding, JWT auth, quote/purchase, or H2 + vector store setup.
