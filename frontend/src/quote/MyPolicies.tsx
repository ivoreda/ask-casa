import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import {
  formatNaira,
  listMyPolicies,
  PRODUCT_LABELS,
  type PolicyResponse,
} from './api'

type MyPoliciesProps = {
  open: boolean
  onClose: () => void
  refreshKey?: number
}

export function MyPolicies({ open, onClose, refreshKey = 0 }: MyPoliciesProps) {
  const { token } = useAuth()
  const [policies, setPolicies] = useState<PolicyResponse[]>([])
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
        const list = await listMyPolicies(token!)
        if (!cancelled) setPolicies(list)
      } catch (err) {
        if (!cancelled) {
          setPolicies([])
          setError(err instanceof Error ? err.message : 'Could not load policies')
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
        aria-labelledby="policies-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="policies-modal-title">My policies</h2>
        <p className="auth-lede">Policies from demo purchases on this account.</p>

        {busy ? <p className="status-line">Loading policies…</p> : null}

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}

        {!busy && !error && policies.length === 0 ? (
          <p className="empty-policies">
            No policies yet. Get a quote and complete demo checkout to see one here.
          </p>
        ) : null}

        {policies.length > 0 ? (
          <ul className="policy-list">
            {policies.map((policy) => (
              <li key={policy.id} className="policy-item">
                <div className="policy-item-head">
                  <strong>{PRODUCT_LABELS[policy.productSlug]}</strong>
                  <span className="policy-status">{policy.status}</span>
                </div>
                <p className="policy-number">{policy.policyNumber}</p>
                <p className="policy-meta">
                  {formatNaira(Number(policy.monthlyPremium))}/mo · Cover{' '}
                  {formatNaira(Number(policy.coverAmount))}
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
