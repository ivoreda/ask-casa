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
