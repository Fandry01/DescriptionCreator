import { useState } from 'react'
import { ApiError } from '../api/descriptionApi'
import { login } from '../api/authApi'
import type { AuthUser } from '../types/auth'

interface LoginPageProps { onAuthenticated: (user: AuthUser) => void }

export function LoginPage({ onAuthenticated }: LoginPageProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    if (loading) return
    setLoading(true)
    setError(null)
    try {
      onAuthenticated(await login(email, password))
    } catch (failure) {
      setError(failure instanceof ApiError && failure.status === 401
        ? 'The email or password is incorrect.'
        : 'Sign in is currently unavailable. Please try again.')
    } finally { setLoading(false) }
  }

  return <main className="login-page">
    <form className="login-card" onSubmit={submit}>
      <span className="brand-monogram" aria-hidden="true">DS</span>
      <p className="eyebrow">Designer Stories</p>
      <h1>Description Creator</h1>
      <p className="login-intro">Sign in to manage and publish product descriptions.</p>
      {error && <div className="inline-error" role="alert">{error}</div>}
      <label className="login-field">Email
        <input autoComplete="email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
      </label>
      <label className="login-field">Password
        <input autoComplete="current-password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
      </label>
      <button className="button button-primary" disabled={loading} type="submit">
        {loading ? 'Signing in…' : 'Sign in'}
      </button>
    </form>
  </main>
}
