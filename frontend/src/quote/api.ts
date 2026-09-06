export type ProductSlug =
  | 'income-protection'
  | 'health-cash'
  | 'device-protection'

export type QuoteRequest = {
  productSlug: ProductSlug
  monthlyIncome?: number
  coverMonths?: number
  dependants?: number
  deviceValue?: number
}

export type QuoteStatus = 'OPEN' | 'PURCHASED'

export type QuoteResponse = {
  id: string
  productSlug: ProductSlug
  inputs: Record<string, unknown>
  monthlyPremium: number
  coverAmount: number
  status: QuoteStatus
  createdAt: string
}

export type PolicyStatus = 'ACTIVE'

export type PolicyResponse = {
  id: string
  quoteId: string
  productSlug: ProductSlug
  holderName: string
  holderEmail: string
  monthlyPremium: number
  coverAmount: number
  status: PolicyStatus
  policyNumber: string
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

export type QuotePreviewResponse = {
  productSlug: ProductSlug
  inputs: Record<string, unknown>
  monthlyPremium: number
  coverAmount: number
}

export async function previewQuote(
  body: QuoteRequest,
): Promise<QuotePreviewResponse> {
  const res = await fetch(`${API_BASE}/api/quotes/preview`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(await parseError(res))
  return (await res.json()) as QuotePreviewResponse
}

export async function createQuote(
  token: string,
  body: QuoteRequest,
): Promise<QuoteResponse> {
  const res = await fetch(`${API_BASE}/api/quotes`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(await parseError(res))
  return (await res.json()) as QuoteResponse
}

export async function purchaseQuote(
  token: string,
  quoteId: string,
): Promise<PolicyResponse> {
  const res = await fetch(`${API_BASE}/api/purchases`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ quoteId }),
  })
  if (!res.ok) throw new Error(await parseError(res))
  return (await res.json()) as PolicyResponse
}

export async function listMyPolicies(token: string): Promise<PolicyResponse[]> {
  const res = await fetch(`${API_BASE}/api/policies/me`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error(await parseError(res))
  return (await res.json()) as PolicyResponse[]
}

export const PRODUCT_LABELS: Record<ProductSlug, string> = {
  'income-protection': 'Income Protection',
  'health-cash': 'Health Cash',
  'device-protection': 'Device Protection',
}

export function formatNaira(amount: number): string {
  return `₦${amount.toLocaleString('en-NG', {
    maximumFractionDigits: 0,
  })}`
}
