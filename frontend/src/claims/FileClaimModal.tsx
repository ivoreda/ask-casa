import { useEffect, useId, useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import {
  fileClaim,
  loadPoliciesForClaim,
  PRODUCT_LABELS,
  type ClaimResponse,
  type PolicyResponse,
} from './api'

type FileClaimModalProps = {
  open: boolean
  onClose: () => void
}

export function FileClaimModal({ open, onClose }: FileClaimModalProps) {
  const { token } = useAuth()
  const descriptionId = useId()
  const policyIdAttr = useId()

  const [policies, setPolicies] = useState<PolicyResponse[]>([])
  const [policyId, setPolicyId] = useState('')
  const [description, setDescription] = useState('')
  const [submitted, setSubmitted] = useState<ClaimResponse | null>(null)
  const [busy, setBusy] = useState(false)
  const [loadingPolicies, setLoadingPolicies] = useState(false)
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
    if (!open) {
      setPolicies([])
      setPolicyId('')
      setDescription('')
      setSubmitted(null)
      setError(null)
      setBusy(false)
      setLoadingPolicies(false)
      return
    }
    if (!token) return

    let cancelled = false
    async function load() {
      setLoadingPolicies(true)
      setError(null)
      try {
        const list = await loadPoliciesForClaim(token!)
        if (cancelled) return
        setPolicies(list)
        setPolicyId(list[0]?.id ?? '')
      } catch (err) {
        if (!cancelled) {
          setPolicies([])
          setError(err instanceof Error ? err.message : 'Could not load policies')
        }
      } finally {
        if (!cancelled) setLoadingPolicies(false)
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [open, token])

  if (!open) return null

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!token) {
      setError('You need to be signed in to file a claim.')
      return
    }
    if (!policyId) {
      setError('Choose a policy to claim against.')
      return
    }
    setError(null)
    setBusy(true)
    try {
      const claim = await fileClaim(token, policyId, description.trim())
      setSubmitted(claim)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Claim failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal quote-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="claim-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        {submitted ? (
          <>
            <h2 id="claim-modal-title">Claim submitted</h2>
            <p className="auth-lede">
              Demo claim only — no real adjudication. Reference{' '}
              <strong>{submitted.claimNumber}</strong> ({submitted.status}).
            </p>
            <p className="policy-meta">{submitted.description}</p>
            <button type="button" className="btn btn-primary" onClick={onClose}>
              Done
            </button>
          </>
        ) : (
          <form className="auth-form" onSubmit={(e) => void handleSubmit(e)}>
            <h2 id="claim-modal-title">File a claim</h2>
            <p className="auth-lede">
              Pick a policy and describe what happened. Demo filing only.
            </p>

            {loadingPolicies ? (
              <p className="status-line">Loading policies…</p>
            ) : null}

            {!loadingPolicies && policies.length === 0 && !error ? (
              <p className="empty-policies">
                You need an active policy first. Get a quote and complete demo
                checkout, then come back to file a claim.
              </p>
            ) : null}

            {policies.length > 0 ? (
              <>
                <label className="field" htmlFor={policyIdAttr}>
                  <span>Policy</span>
                  <select
                    id={policyIdAttr}
                    value={policyId}
                    onChange={(e) => setPolicyId(e.target.value)}
                    disabled={busy}
                    required
                  >
                    {policies.map((policy) => (
                      <option key={policy.id} value={policy.id}>
                        {PRODUCT_LABELS[policy.productSlug]} · {policy.policyNumber}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="field" htmlFor={descriptionId}>
                  <span>What happened</span>
                  <textarea
                    id={descriptionId}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    rows={4}
                    minLength={10}
                    required
                    disabled={busy}
                    placeholder="Briefly describe the incident (at least 10 characters)"
                  />
                </label>
              </>
            ) : null}

            {error ? (
              <p className="form-error" role="alert">
                {error}
              </p>
            ) : null}

            <div className="quote-actions">
              <button type="button" className="btn btn-ghost" onClick={onClose} disabled={busy}>
                Cancel
              </button>
              {policies.length > 0 ? (
                <button type="submit" className="btn btn-primary" disabled={busy}>
                  {busy ? 'Submitting…' : 'Submit claim'}
                </button>
              ) : (
                <button type="button" className="btn btn-primary" onClick={onClose}>
                  Close
                </button>
              )}
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
