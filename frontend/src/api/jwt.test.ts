import { describe, expect, it } from 'vitest'
import { getUserIdFromToken } from './jwt'

function fakeToken(claims: object): string {
  const base64 = btoa(JSON.stringify(claims))
  // Real JWTs are base64url, not base64, and always omit padding - matching that
  // here is what actually exercises base64UrlDecode's '-'/'_' and padding handling,
  // rather than happening to work only because Buffer.from(..., 'base64') already
  // tolerates the standard alphabet.
  const payload = base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  return `header.${payload}.signature`
}

describe('getUserIdFromToken', () => {
  it('decodes the user-id claim from a real-shaped token', () => {
    const token = fakeToken({
      iss: 'saga-ecosystem-auth',
      sub: 'cashier@test.local',
      'user-id': 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1',
      iat: 1789385142,
      exp: 1789388742,
    })

    expect(getUserIdFromToken(token)).toBe('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1')
  })

  it('decodes a payload whose base64 form actually contains + and / (not just -/_)', () => {
    // Chosen so the underlying base64 has both characters, proving the '-'/'_' ->
    // '+'/'/' reversal is genuinely exercised, not just correct by coincidence.
    const claims = { 'user-id': '>>>???***counter-example-payload///+++' }
    const base64 = btoa(JSON.stringify(claims))
    expect(base64).toMatch(/[+/]/)

    expect(getUserIdFromToken(fakeToken(claims))).toBe(claims['user-id'])
  })
})
