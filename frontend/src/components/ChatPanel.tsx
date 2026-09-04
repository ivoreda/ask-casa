import { useRef, useState, type FormEvent } from 'react'
import { getSessionId, streamChat, type Citation } from '../api/chat'
import { MessageList, type ChatMessage } from './MessageList'

const SUGGESTIONS = [
  'What’s covered under Device Protection?',
  'Compare Income Protection and Health Cash',
  'What are exclusions for Health Cash?',
  'What’s on my policy?',
]

export function ChatPanel() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [status, setStatus] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const abortRef = useRef<AbortController | null>(null)

  async function send(text: string) {
    const trimmed = text.trim()
    if (!trimmed || busy) return

    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller

    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: trimmed,
    }
    const assistantId = crypto.randomUUID()
    const assistantMessage: ChatMessage = {
      id: assistantId,
      role: 'assistant',
      content: '',
      streaming: true,
    }

    setMessages((prev) => [...prev, userMessage, assistantMessage])
    setInput('')
    setBusy(true)
    setStatus(null)

    try {
      await streamChat(
        getSessionId(),
        trimmed,
        {
          onStatus: (message) => setStatus(message),
          onToken: (token) => {
            setStatus(null)
            setMessages((prev) =>
              prev.map((m) =>
                m.id === assistantId ? { ...m, content: m.content + token } : m,
              ),
            )
          },
          onDone: (citations: Citation[]) => {
            setMessages((prev) =>
              prev.map((m) =>
                m.id === assistantId
                  ? { ...m, streaming: false, citations }
                  : m,
              ),
            )
            setStatus(null)
          },
          onError: (message) => {
            setMessages((prev) =>
              prev.map((m) =>
                m.id === assistantId
                  ? {
                      ...m,
                      streaming: false,
                      error: true,
                      content: m.content || message,
                    }
                  : m,
              ),
            )
            setStatus(null)
          },
        },
        controller.signal,
      )
    } catch (err) {
      if ((err as Error).name !== 'AbortError') {
        setMessages((prev) =>
          prev.map((m) =>
            m.id === assistantId
              ? {
                  ...m,
                  streaming: false,
                  error: true,
                  content: m.content || 'Something went wrong. Try again.',
                }
              : m,
          ),
        )
      }
    } finally {
      setBusy(false)
      setMessages((prev) =>
        prev.map((m) => (m.id === assistantId ? { ...m, streaming: false } : m)),
      )
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    void send(input)
  }

  return (
    <section className="chat-panel" aria-label="Ask Casa">
      <MessageList messages={messages} status={status} />

      <div className="suggestions">
        {SUGGESTIONS.map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            className="suggestion"
            disabled={busy}
            onClick={() => void send(suggestion)}
          >
            {suggestion}
          </button>
        ))}
      </div>

      <form className="composer" onSubmit={onSubmit}>
        <label className="sr-only" htmlFor="chat-input">
          Message
        </label>
        <input
          id="chat-input"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask about cover, limits, exclusions…"
          disabled={busy}
          autoComplete="off"
        />
        <button type="submit" className="btn btn-primary" disabled={busy || !input.trim()}>
          Send
        </button>
      </form>
    </section>
  )
}
