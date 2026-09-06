import { useEffect, useId, useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { CheckoutPanel } from './CheckoutPanel'
import {
  createQuote,
  formatNaira,
  previewQuote,
  PRODUCT_LABELS,
  type ProductSlug,
  type QuotePreviewResponse,
  type QuoteRequest,
  type QuoteResponse,
} from './api'

type QuoteModalProps = {
  open: boolean
  onClose: () => void
  onPurchased?: () => void
  onNeedAuth?: (mode: 'login' | 'register') => void
}

const PRODUCTS: ProductSlug[] = [
  'income-protection',
  'health-cash',
  'device-protection',
]

type Step = 'form' | 'checkout'

export function QuoteModal({
  open,
  onClose,
  onPurchased,
  onNeedAuth,
}: QuoteModalProps) {
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
  const [savedQuote, setSavedQuote] = useState<QuoteResponse | null>(null)
  const [preview, setPreview] = useState<QuotePreviewResponse | null>(null)
  const [pendingRequest, setPendingRequest] = useState<QuoteRequest | null>(null)
  const [wantsCheckout, setWantsCheckout] = useState(false)
  const [step, setStep] = useState<Step>('form')
  const [purchased, setPurchased] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const displayQuote = savedQuote ?? preview

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
      setSavedQuote(null)
      setPreview(null)
      setPendingRequest(null)
      setWantsCheckout(false)
      setStep('form')
      setPurchased(false)
      setError(null)
      setBusy(false)
    }
  }, [open])

  useEffect(() => {
    if (!wantsCheckout || !token || !pendingRequest) return
    let cancelled = false
    async function promote() {
      setBusy(true)
      setError(null)
      try {
        const created = await createQuote(token!, pendingRequest!)
        if (!cancelled) {
          setSavedQuote(created)
          setPreview(null)
          setStep('checkout')
          setWantsCheckout(false)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Could not save quote')
          setWantsCheckout(false)
        }
      } finally {
        if (!cancelled) setBusy(false)
      }
    }
    void promote()
    return () => {
      cancelled = true
    }
  }, [wantsCheckout, token, pendingRequest])

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

  function clearQuoteResult() {
    setSavedQuote(null)
    setPreview(null)
    setPendingRequest(null)
  }

  async function handleQuote(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    const request = buildRequest()
    try {
      if (token) {
        const created = await createQuote(token, request)
        setSavedQuote(created)
        setPreview(null)
        setPendingRequest(null)
      } else {
        const result = await previewQuote(request)
        setPreview(result)
        setSavedQuote(null)
        setPendingRequest(request)
      }
    } catch (err) {
      clearQuoteResult()
      setError(err instanceof Error ? err.message : 'Quote failed')
    } finally {
      setBusy(false)
    }
  }

  async function continueToBuy() {
    if (!token) {
      setWantsCheckout(true)
      onNeedAuth?.('register')
      return
    }
    if (savedQuote) {
      setStep('checkout')
      return
    }
    if (!pendingRequest) {
      setError('Calculate a premium before continuing.')
      return
    }
    setError(null)
    setBusy(true)
    try {
      const created = await createQuote(token, pendingRequest)
      setSavedQuote(created)
      setPreview(null)
      setStep('checkout')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save quote')
    } finally {
      setBusy(false)
    }
  }

  function selectProduct(slug: ProductSlug) {
    setProductSlug(slug)
    clearQuoteResult()
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
        {step === 'checkout' && savedQuote ? (
          <CheckoutPanel
            quote={savedQuote}
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
                      clearQuoteResult()
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
                      clearQuoteResult()
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
                    clearQuoteResult()
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
                    clearQuoteResult()
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

            {displayQuote ? (
              <dl className="quote-summary">
                <div>
                  <dt>Product</dt>
                  <dd>{PRODUCT_LABELS[displayQuote.productSlug]}</dd>
                </div>
                <div>
                  <dt>Monthly premium</dt>
                  <dd>{formatNaira(Number(displayQuote.monthlyPremium))}</dd>
                </div>
                <div>
                  <dt>Cover amount</dt>
                  <dd>{formatNaira(Number(displayQuote.coverAmount))}</dd>
                </div>
              </dl>
            ) : null}

            <div className="quote-actions">
              <button type="submit" className="btn btn-primary" disabled={busy}>
                {busy
                  ? 'Calculating…'
                  : displayQuote
                    ? 'Recalculate'
                    : 'Calculate premium'}
              </button>
              {displayQuote ? (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => void continueToBuy()}
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
