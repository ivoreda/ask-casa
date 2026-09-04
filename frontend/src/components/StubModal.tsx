type StubModalProps = {
  open: boolean
  onClose: () => void
}

export function StubModal({ open, onClose }: StubModalProps) {
  if (!open) return null

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="stub-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="stub-modal-title">Register to continue</h2>
        <p>
          Quotes, claims, and personal policy details need an account. This demo
          keeps Ask Casa focused on product knowledge — auth is stubbed for now.
        </p>
        <button type="button" className="btn btn-primary" onClick={onClose}>
          Got it
        </button>
      </div>
    </div>
  )
}
