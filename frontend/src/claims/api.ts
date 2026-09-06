import {
  listMyPolicies,
  PRODUCT_LABELS,
  type PolicyResponse,
} from '../quote/api'

export type ClaimResponse = {
  id: string
  policyId: string
  description: string
  status: string
  claimNumber: string
  createdAt: string
}

const API_BASE =
  (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ||
  'http://localhost:8080'

async function parseError(res: Response): Promise<string> {
  try {
    const body = (await res.json()) as { message?: string }
    if (body.message) return body.message
  } catch {
    // ignore non-JSON
  }
  return `Request failed (${res.status})`
}

export async function fileClaim(
  token: string,
  policyId: string,
  description: string,
): Promise<ClaimResponse> {
  const res = await fetch(`${API_BASE}/api/claims`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ policyId, description }),
  })
  if (!res.ok) throw new Error(await parseError(res))
  return (await res.json()) as ClaimResponse
}

export async function loadPoliciesForClaim(
  token: string,
): Promise<PolicyResponse[]> {
  return listMyPolicies(token)
}

export { PRODUCT_LABELS }
export type { PolicyResponse }
