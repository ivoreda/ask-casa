import { useState } from 'react'
import { AuthModal, type AuthMode } from './auth/AuthModal'
import { useAuth } from './auth/AuthContext'
import { ChatPanel } from './components/ChatPanel'
import { StubModal } from './components/StubModal'
import { MyPolicies } from './quote/MyPolicies'
import { QuoteModal } from './quote/QuoteModal'

export default function App() {
  const { user, logout, ready } = useAuth()
  const [claimStubOpen, setClaimStubOpen] = useState(false)
  const [quoteOpen, setQuoteOpen] = useState(false)
  const [policiesOpen, setPoliciesOpen] = useState(false)
  const [policiesRefresh, setPoliciesRefresh] = useState(0)
  const [authOpen, setAuthOpen] = useState(false)
  const [authMode, setAuthMode] = useState<AuthMode>('login')

  function openAuth(mode: AuthMode) {
    setAuthMode(mode)
    setAuthOpen(true)
  }

  function onGetQuote() {
    if (!user) {
      openAuth('register')
      return
    }
    setQuoteOpen(true)
  }

  function onMyPolicies() {
    if (!user) {
      openAuth('login')
      return
    }
    setPoliciesOpen(true)
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">Casava</div>
        <nav className="nav" aria-label="Primary">
          <button type="button" className="nav-link" onClick={onGetQuote}>
            Get a quote
          </button>
          <button type="button" className="nav-link" onClick={() => setClaimStubOpen(true)}>
            File a claim
          </button>
          {ready && user ? (
            <>
              <button type="button" className="nav-link" onClick={onMyPolicies}>
                My policies
              </button>
              <span className="nav-user" title={user.email}>
                {user.name}
              </span>
              <button type="button" className="nav-link" onClick={logout}>
                Log out
              </button>
            </>
          ) : ready ? (
            <>
              <button type="button" className="nav-link" onClick={() => openAuth('register')}>
                Register
              </button>
              <button type="button" className="nav-link" onClick={() => openAuth('login')}>
                Log in
              </button>
            </>
          ) : null}
        </nav>
      </header>

      <main className="shell">
        <section className="intro">
          <p className="eyebrow">Ask Casa</p>
          <h1>Insurance, finally enjoyable.</h1>
          <p className="lede">
            Product answers grounded in Casava’s demo knowledge base — cover,
            limits, and exclusions, streamed live.
          </p>
        </section>

        <ChatPanel />
      </main>

      <AuthModal
        open={authOpen}
        mode={authMode}
        onModeChange={setAuthMode}
        onClose={() => setAuthOpen(false)}
      />
      <StubModal
        open={claimStubOpen}
        onClose={() => setClaimStubOpen(false)}
        title="File a claim"
        message="Claims filing isn’t available in this demo yet. Ask Casa can still explain cover and exclusions."
      />
      <QuoteModal
        open={quoteOpen}
        onClose={() => setQuoteOpen(false)}
        onPurchased={() => setPoliciesRefresh((n) => n + 1)}
      />
      <MyPolicies
        open={policiesOpen}
        onClose={() => setPoliciesOpen(false)}
        refreshKey={policiesRefresh}
      />
    </div>
  )
}
