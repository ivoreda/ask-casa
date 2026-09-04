export type Citation = {
  title: string
  sourceType: string
  productName: string
  productSlug: string
}

export type ChatHandlers = {
  onStatus?: (message: string) => void
  onToken: (text: string) => void
  onDone: (citations: Citation[]) => void
  onError: (message: string) => void
}

const API_BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '')
  || 'http://localhost:8080'
const API_URL = `${API_BASE}/api/chat`
const SESSION_KEY = 'casava-session-id'

export function getSessionId(): string {
  const existing = sessionStorage.getItem(SESSION_KEY)
  if (existing) return existing
  const id = crypto.randomUUID()
  sessionStorage.setItem(SESSION_KEY, id)
  return id
}

type SsePayload = {
  type?: string
  message?: string
  text?: string
  citations?: Citation[]
}

export async function streamChat(
  sessionId: string,
  message: string,
  handlers: ChatHandlers,
  signal?: AbortSignal,
): Promise<void> {
  let res: Response
  try {
    res = await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify({ sessionId, message }),
      signal,
    })
  } catch {
    handlers.onError('Cannot reach the API. Is the backend running on port 8080?')
    return
  }

  if (!res.ok || !res.body) {
    handlers.onError(`Chat failed (${res.status})`)
    return
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    const parts = buffer.split('\n\n')
    buffer = parts.pop() ?? ''

    for (const part of parts) {
      dispatchSsePart(part, handlers)
    }
  }

  if (buffer.trim()) {
    dispatchSsePart(buffer, handlers)
  }
}

function dispatchSsePart(part: string, handlers: ChatHandlers): void {
  const lines = part.split('\n')
  let eventName = 'message'
  const dataLines: string[] = []

  for (const line of lines) {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }

  if (dataLines.length === 0) return

  let payload: SsePayload
  try {
    payload = JSON.parse(dataLines.join('\n')) as SsePayload
  } catch {
    handlers.onError('Received a malformed response from the server.')
    return
  }

  const type = payload.type ?? eventName

  if (type === 'status' && payload.message) {
    handlers.onStatus?.(payload.message)
  } else if (type === 'token' && payload.text != null) {
    handlers.onToken(payload.text)
  } else if (type === 'done') {
    handlers.onDone(payload.citations ?? [])
  } else if (type === 'error') {
    handlers.onError(payload.message ?? 'Something went wrong.')
  }
}
