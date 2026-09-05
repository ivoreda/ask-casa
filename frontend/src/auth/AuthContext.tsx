import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import {
  loginRequest,
  meRequest,
  registerRequest,
  type User,
} from './api'

const TOKEN_KEY = 'casava-auth-token'

type AuthContextValue = {
  user: User | null
  token: string | null
  ready: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, name: string) => Promise<void>
  logout: () => void
  authHeaders: () => Record<string, string>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null)
  const [user, setUser] = useState<User | null>(null)
  const [ready, setReady] = useState(false)

  const persist = useCallback((nextToken: string, nextUser: User) => {
    sessionStorage.setItem(TOKEN_KEY, nextToken)
    setToken(nextToken)
    setUser(nextUser)
  }, [])

  const clear = useCallback(() => {
    sessionStorage.removeItem(TOKEN_KEY)
    setToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    let cancelled = false
    const stored = sessionStorage.getItem(TOKEN_KEY)

    async function hydrate() {
      if (!stored) {
        if (!cancelled) setReady(true)
        return
      }
      try {
        const me = await meRequest(stored)
        if (!cancelled) {
          setToken(stored)
          setUser(me)
        }
      } catch {
        sessionStorage.removeItem(TOKEN_KEY)
        if (!cancelled) {
          setToken(null)
          setUser(null)
        }
      } finally {
        if (!cancelled) setReady(true)
      }
    }

    void hydrate()
    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await loginRequest(email, password)
      persist(res.token, res.user)
    },
    [persist],
  )

  const register = useCallback(
    async (email: string, password: string, name: string) => {
      const res = await registerRequest(email, password, name)
      persist(res.token, res.user)
    },
    [persist],
  )

  const logout = useCallback(() => {
    clear()
  }, [clear])

  const authHeaders = useCallback((): Record<string, string> => {
    if (!token) return {}
    return { Authorization: `Bearer ${token}` }
  }, [token])

  const value = useMemo(
    () => ({
      user,
      token,
      ready,
      login,
      register,
      logout,
      authHeaders,
    }),
    [user, token, ready, login, register, logout, authHeaders],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
