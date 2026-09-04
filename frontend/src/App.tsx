import { useState } from 'react'
import { ChatPanel } from './components/ChatPanel'
import { StubModal } from './components/StubModal'

export default function App() {
  const [stubOpen, setStubOpen] = useState(false)

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">Casava</div>
        <nav className="nav">
          <button type="button" className="nav-link" onClick={() => setStubOpen(true)}>
            Get a quote
          </button>
          <button type="button" className="nav-link" onClick={() => setStubOpen(true)}>
            File a claim
          </button>
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

      <StubModal open={stubOpen} onClose={() => setStubOpen(false)} />
    </div>
  )
}
