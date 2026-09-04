import type { Citation } from '../api/chat'

export type ChatMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  citations?: Citation[]
  streaming?: boolean
  error?: boolean
}

type MessageListProps = {
  messages: ChatMessage[]
  status?: string | null
}

export function MessageList({ messages, status }: MessageListProps) {
  return (
    <div className="message-list" aria-live="polite">
      {messages.length === 0 && (
        <p className="message-empty">Ask Casa about cover, limits, or exclusions.</p>
      )}
      {messages.map((message) => (
        <article
          key={message.id}
          className={`message message-${message.role}${message.error ? ' message-error' : ''}`}
        >
          <div className="message-role">{message.role === 'user' ? 'You' : 'Casa'}</div>
          <div className="message-body">
            {message.content}
            {message.streaming && <span className="cursor" aria-hidden="true" />}
          </div>
          {message.citations && message.citations.length > 0 && (
            <ul className="citations">
              {message.citations.map((citation, index) => (
                <li key={`${citation.productSlug}-${citation.sourceType}-${index}`}>
                  <span className="citation-chip">
                    {citation.productName} · {citation.sourceType}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </article>
      ))}
      {status && <p className="status-line">{status}</p>}
    </div>
  )
}
