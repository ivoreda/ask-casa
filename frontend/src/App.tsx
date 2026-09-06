import { useState } from 'react'
import { AuthModal, type AuthMode } from './auth/AuthModal'
import { useAuth } from './auth/AuthContext'
import { FileClaimModal } from './claims/FileClaimModal'
import { MyClaims } from './claims/MyClaims'
import { ChatPanel } from './components/ChatPanel'
import { MyPolicies } from './quote/MyPolicies'
import { QuoteModal } from './quote/QuoteModal'

export default function App() {
  const { user, logout, ready } = useAuth()
  const [claimOpen, setClaimOpen] = useState(false)
  const [quoteOpen, setQuoteOpen] = useState(false)
  const [policiesOpen, setPoliciesOpen] = useState(false)
  const [policiesRefresh, setPoliciesRefresh] = useState(0)
  const [claimsOpen, setClaimsOpen] = useState(false)
  const [claimsRefresh, setClaimsRefresh] = useState(0)
  const [authOpen, setAuthOpen] = useState(false)
  const [authMode, setAuthMode] = useState<AuthMode>('login')

  function openAuth(mode: AuthMode) {
    setAuthMode(mode)
    setAuthOpen(true)
  }

  function onGetQuote() {
    setQuoteOpen(true)
  }

  function onFileClaim() {
    if (!user) {
      openAuth('login')
      return
    }
    setClaimOpen(true)
  }

  function onMyPolicies() {
    if (!user) {
      openAuth('login')
      return
    }
    setPoliciesOpen(true)
  }

  function onMyClaims() {
    if (!user) {
      openAuth('login')
      return
    }
    setClaimsOpen(true)
  }

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">Casava</div>
        <nav className="nav" aria-label="Primary">
          <button type="button" className="nav-link" onClick={onGetQuote}>
            Get a quote
          </button>
          <button type="button" className="nav-link" onClick={onFileClaim}>
            File a claim
          </button>
          {ready && user ? (
            <>
              <button type="button" className="nav-link" onClick={onMyPolicies}>
                My policies
              </button>
              <button type="button" className="nav-link" onClick={onMyClaims}>
                My claims
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
      <FileClaimModal
        open={claimOpen}
        onClose={() => setClaimOpen(false)}
        onFiled={() => setClaimsRefresh((n) => n + 1)}
      />
      <QuoteModal
        open={quoteOpen}
        onClose={() => setQuoteOpen(false)}
        onPurchased={() => setPoliciesRefresh((n) => n + 1)}
        onNeedAuth={openAuth}
      />
      <MyPolicies
        open={policiesOpen}
        onClose={() => setPoliciesOpen(false)}
        refreshKey={policiesRefresh}
      />
      <MyClaims
        open={claimsOpen}
        onClose={() => setClaimsOpen(false)}
        refreshKey={claimsRefresh}
      />
    </div>
  )
}
