import { useEffect } from 'react'
import { LoginForm } from './LoginForm'
import { RegisterForm } from './RegisterForm'

export type AuthMode = 'login' | 'register'

type AuthModalProps = {
  open: boolean
  mode: AuthMode
  onModeChange: (mode: AuthMode) => void
  onClose: () => void
}

export function AuthModal({ open, mode, onModeChange, onClose }: AuthModalProps) {
  useEffect(() => {
    if (!open) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="modal auth-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        {mode === 'login' ? (
          <LoginForm
            onSuccess={onClose}
            onSwitchToRegister={() => onModeChange('register')}
          />
        ) : (
          <RegisterForm
            onSuccess={onClose}
            onSwitchToLogin={() => onModeChange('login')}
          />
        )}
        <button type="button" className="btn btn-ghost" onClick={onClose}>
          Cancel
        </button>
      </div>
    </div>
  )
}
