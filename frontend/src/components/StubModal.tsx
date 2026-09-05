type StubModalProps = {
  open: boolean
  onClose: () => void
  title?: string
  message?: string
}

export function StubModal({
  open,
  onClose,
  title = 'Coming soon',
  message = 'This demo path is still stubbed. Ask Casa can still answer product questions.',
}: StubModalProps) {
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
        <h2 id="stub-modal-title">{title}</h2>
        <p>{message}</p>
        <button type="button" className="btn btn-primary" onClick={onClose}>
          Got it
        </button>
      </div>
    </div>
  )
}
