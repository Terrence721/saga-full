import { apiFetch } from './httpClient'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  type: string
  expiresIn: number
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}
