import { useEffect, useId, useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { CheckoutPanel } from './CheckoutPanel'
import {
  createQuote,
  formatNaira,
  PRODUCT_LABELS,
  type ProductSlug,
  type QuoteRequest,
  type QuoteResponse,
} from './api'

type QuoteModalProps = {
  open: boolean
  onClose: () => void
  onPurchased?: () => void
}

const PRODUCTS: ProductSlug[] = [
  'income-protection',
  'health-cash',
  'device-protection',
]

type Step = 'form' | 'checkout'

export function QuoteModal({ open, onClose, onPurchased }: QuoteModalProps) {
  const { token } = useAuth()
  const incomeId = useId()
  const monthsId = useId()
  const dependantsId = useId()
  const deviceId = useId()

  const [productSlug, setProductSlug] = useState<ProductSlug>('income-protection')
  const [monthlyIncome, setMonthlyIncome] = useState('500000')
  const [coverMonths, setCoverMonths] = useState('6')
  const [dependants, setDependants] = useState('0')
  const [deviceValue, setDeviceValue] = useState('300000')
  const [quote, setQuote] = useState<QuoteResponse | null>(null)
  const [step, setStep] = useState<Step>('form')
  const [purchased, setPurchased] = useState(false)
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
    if (!open) {
      setQuote(null)
      setStep('form')
      setPurchased(false)
      setError(null)
      setBusy(false)
    }
  }, [open])

  if (!open) return null

  function buildRequest(): QuoteRequest {
    switch (productSlug) {
      case 'income-protection':
        return {
          productSlug,
          monthlyIncome: Number(monthlyIncome),
          coverMonths: Number(coverMonths),
        }
      case 'health-cash':
        return {
          productSlug,
          dependants: Number(dependants),
        }
      case 'device-protection':
        return {
          productSlug,
          deviceValue: Number(deviceValue),
        }
    }
  }

  async function handleQuote(e: FormEvent) {
    e.preventDefault()
    if (!token) {
      setError('You need to be signed in to get a quote.')
      return
    }
    setError(null)
    setBusy(true)
    try {
      const created = await createQuote(token, buildRequest())
      setQuote(created)
    } catch (err) {
      setQuote(null)
      setError(err instanceof Error ? err.message : 'Quote failed')
    } finally {
      setBusy(false)
    }
  }

  function selectProduct(slug: ProductSlug) {
    setProductSlug(slug)
    setQuote(null)
    setError(null)
    setStep('form')
    setPurchased(false)
  }

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal quote-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="quote-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        {step === 'checkout' && quote ? (
          <CheckoutPanel
            quote={quote}
            onBack={() => setStep('form')}
            onDone={() => {
              setPurchased(true)
              onPurchased?.()
            }}
          />
        ) : (
          <form className="auth-form" onSubmit={(e) => void handleQuote(e)}>
            <h2 id="quote-modal-title">Get a quote</h2>
            <p className="auth-lede">
              Demo pricing only — not binding underwriting.
            </p>

            <div className="product-tabs" role="tablist" aria-label="Products">
              {PRODUCTS.map((slug) => (
                <button
                  key={slug}
                  type="button"
                  role="tab"
                  aria-selected={productSlug === slug}
                  className={
                    productSlug === slug ? 'product-tab active' : 'product-tab'
                  }
                  onClick={() => selectProduct(slug)}
                  disabled={busy}
                >
                  {PRODUCT_LABELS[slug]}
                </button>
              ))}
            </div>

            {productSlug === 'income-protection' ? (
              <>
                <label className="field" htmlFor={incomeId}>
                  <span>Monthly income (₦)</span>
                  <input
                    id={incomeId}
                    type="number"
                    min={1}
                    step={1000}
                    required
                    value={monthlyIncome}
                    onChange={(e) => {
                      setMonthlyIncome(e.target.value)
                      setQuote(null)
                    }}
                    disabled={busy}
                  />
                </label>
                <label className="field" htmlFor={monthsId}>
                  <span>Cover months (3–12)</span>
                  <input
                    id={monthsId}
                    type="number"
                    min={3}
                    max={12}
                    required
                    value={coverMonths}
                    onChange={(e) => {
                      setCoverMonths(e.target.value)
                      setQuote(null)
                    }}
                    disabled={busy}
                  />
                </label>
              </>
            ) : null}

            {productSlug === 'health-cash' ? (
              <label className="field" htmlFor={dependantsId}>
                <span>Dependants (0–4)</span>
                <input
                  id={dependantsId}
                  type="number"
                  min={0}
                  max={4}
                  required
                  value={dependants}
                  onChange={(e) => {
                    setDependants(e.target.value)
                    setQuote(null)
                  }}
                  disabled={busy}
                />
              </label>
            ) : null}

            {productSlug === 'device-protection' ? (
              <label className="field" htmlFor={deviceId}>
                <span>Device value (₦)</span>
                <input
                  id={deviceId}
                  type="number"
                  min={1}
                  step={1000}
                  required
                  value={deviceValue}
                  onChange={(e) => {
                    setDeviceValue(e.target.value)
                    setQuote(null)
                  }}
                  disabled={busy}
                />
              </label>
            ) : null}

            {error ? (
              <p className="form-error" role="alert">
                {error}
              </p>
            ) : null}

            {quote ? (
              <dl className="quote-summary">
                <div>
                  <dt>Product</dt>
                  <dd>{PRODUCT_LABELS[quote.productSlug]}</dd>
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
            ) : null}

            <div className="quote-actions">
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {busy ? 'Calculating…' : quote ? 'Recalculate' : 'Calculate premium'}
              </button>
              {quote ? (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => setStep('checkout')}
                  disabled={busy}
                >
                  Continue to buy
                </button>
              ) : null}
            </div>
          </form>
        )}

        <button type="button" className="btn btn-ghost" onClick={onClose}>
          {purchased ? 'Close' : 'Cancel'}
        </button>
      </div>
    </div>
  )
}
