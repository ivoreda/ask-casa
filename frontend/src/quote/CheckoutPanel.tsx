import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import {
  formatNaira,
  PRODUCT_LABELS,
  purchaseQuote,
  type PolicyResponse,
  type QuoteResponse,
} from './api'

type CheckoutPanelProps = {
  quote: QuoteResponse
  onBack: () => void
  onDone: (policy: PolicyResponse) => void
}

export function CheckoutPanel({ quote, onBack, onDone }: CheckoutPanelProps) {
  const { token, user } = useAuth()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [policy, setPolicy] = useState<PolicyResponse | null>(null)

  async function pay() {
    if (!token) {
      setError('You need to be signed in to buy.')
      return
    }
    setError(null)
    setBusy(true)
    try {
      const created = await purchaseQuote(token, quote.id)
      setPolicy(created)
      onDone(created)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Purchase failed')
    } finally {
      setBusy(false)
    }
  }

  if (policy) {
    return (
      <div className="checkout-panel">
        <h2 id="quote-modal-title">You’re covered</h2>
        <p className="auth-lede">
          Demo purchase complete — no real payment was taken.
        </p>
        <dl className="quote-summary">
          <div>
            <dt>Policy number</dt>
            <dd className="policy-number">{policy.policyNumber}</dd>
          </div>
          <div>
            <dt>Product</dt>
            <dd>{PRODUCT_LABELS[policy.productSlug]}</dd>
          </div>
          <div>
            <dt>Monthly premium</dt>
            <dd>{formatNaira(Number(policy.monthlyPremium))}</dd>
          </div>
          <div>
            <dt>Cover amount</dt>
            <dd>{formatNaira(Number(policy.coverAmount))}</dd>
          </div>
        </dl>
      </div>
    )
  }

  return (
    <div className="checkout-panel">
      <h2 id="quote-modal-title">Checkout</h2>
      <p className="auth-lede">
        Confirm details and pay with the demo button — not a real charge.
      </p>

      <dl className="quote-summary">
        <div>
          <dt>Product</dt>
          <dd>{PRODUCT_LABELS[quote.productSlug]}</dd>
        </div>
        <div>
          <dt>Policyholder</dt>
          <dd>
            {user?.name}
            <span className="muted-inline"> · {user?.email}</span>
          </dd>
        </div>
        <div>
          <dt>Monthly premium</dt>
          <dd>{formatNaira(Number(quote.monthlyPremium))}</dd>
        </div>
        <div>
          <dt>Cover amount</dt>
          <dd>{formatNaira(Number(quote.coverAmount))}</dd>
        </div>
      </dl>

      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="quote-actions">
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => void pay()}
          disabled={busy}
        >
          {busy ? 'Processing…' : 'Pay (demo)'}
        </button>
        <button
          type="button"
          className="btn btn-ghost"
          onClick={onBack}
          disabled={busy}
        >
          Back to quote
        </button>
      </div>
    </div>
  )
}
