import { useId, useState, type FormEvent } from 'react'
import { useAuth } from './AuthContext'

type RegisterFormProps = {
  onSuccess: () => void
  onSwitchToLogin: () => void
}

export function RegisterForm({ onSuccess, onSwitchToLogin }: RegisterFormProps) {
  const { register } = useAuth()
  const nameId = useId()
  const emailId = useId()
  const passwordId = useId()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await register(email.trim(), password, name.trim())
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="auth-form" onSubmit={(e) => void handleSubmit(e)}>
      <h2 id="auth-modal-title">Create account</h2>
      <p className="auth-lede">
        Register to get quotes, buy a demo policy, and ask Casa about your cover.
      </p>

      <label className="field" htmlFor={nameId}>
        <span>Name</span>
        <input
          id={nameId}
          type="text"
          autoComplete="name"
          required
          value={name}
          onChange={(e) => setName(e.target.value)}
          disabled={busy}
        />
      </label>

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
          autoComplete="new-password"
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
        {busy ? 'Creating…' : 'Register'}
      </button>

      <p className="auth-switch">
        Already registered?{' '}
        <button type="button" className="text-link" onClick={onSwitchToLogin} disabled={busy}>
          Log in
        </button>
      </p>
    </form>
  )
}
