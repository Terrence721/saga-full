interface TokenClaims {
  'user-id': string
}

// JWTs are signed, not encrypted - decoding the payload client-side to read a
// claim is standard practice, not a security boundary. The signature is
// still verified server-side on every request that matters (JwtPerimeterGuard).
function base64UrlDecode(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
  return atob(padded)
}

export function getUserIdFromToken(token: string): string {
  const payload = token.split('.')[1]
  const claims = JSON.parse(base64UrlDecode(payload)) as TokenClaims
  return claims['user-id']
}
