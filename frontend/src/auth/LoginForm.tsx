import { useId, useState, type FormEvent } from 'react'
import { useAuth } from './AuthContext'

type LoginFormProps = {
  onSuccess: () => void
  onSwitchToRegister: () => void
}

export function LoginForm({ onSuccess, onSwitchToRegister }: LoginFormProps) {
  const { login } = useAuth()
  const emailId = useId()
  const passwordId = useId()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await login(email.trim(), password)
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={(e) => void handleSubmit(e)}>
      <h2 id="auth-modal-title">Log in</h2>
      <p className="auth-lede">Welcome back — pick up quotes and policies where you left off.</p>

      <label className="field" htmlFor={emailId}>
        <span>Email</span>
        <input
          id={emailId}
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          disabled={busy}
        />
      </label>

      <label className="field" htmlFor={passwordId}>
        <span>Password</span>
        <input
          id={passwordId}
          type="password"
          autoComplete="current-password"
          required
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          disabled={busy}
        />
      </label>

      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}

      <button type="submit" className="btn btn-primary" disabled={busy}>
        {busy ? 'Signing in…' : 'Log in'}
      </button>

      <p className="auth-switch">
        New here?{' '}
        <button type="button" className="text-link" onClick={onSwitchToRegister} disabled={busy}>
          Create an account
        </button>
      </p>
    </form>
  )
}
