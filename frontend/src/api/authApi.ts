import { clearCsrfToken, ensureCsrfToken, requestJson } from './descriptionApi'
import type { AuthUser, UsageStatus } from '../types/auth'

export function getCurrentUser(): Promise<AuthUser> {
  return requestJson<AuthUser>('/api/auth/me')
}

export async function login(email: string, password: string): Promise<AuthUser> {
  await ensureCsrfToken()
  const user = await requestJson<AuthUser>('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  clearCsrfToken()
  await ensureCsrfToken()
  return user
}

export async function logout(): Promise<void> {
  await requestJson<void>('/api/auth/logout', { method: 'POST' })
  clearCsrfToken()
}

export function getUsage(): Promise<UsageStatus> {
  return requestJson<UsageStatus>('/api/usage')
}
