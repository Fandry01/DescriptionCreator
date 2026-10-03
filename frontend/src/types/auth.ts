export interface AuthUser {
  authenticated: true
  email: string
  displayName: string | null
}

export interface UsageStatus {
  used: number
  limit: number
  remaining: number
  period: string
}
