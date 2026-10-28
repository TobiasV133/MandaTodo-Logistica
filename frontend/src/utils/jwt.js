export function decodeJwtPayload(token) {
  if (typeof token !== 'string') return null

  const partes = token.split('.')
  if (partes.length !== 3) return null

  try {
    const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/')
    const padding = '='.repeat((4 - (base64.length % 4)) % 4)
    const binario = atob(base64 + padding)
    const bytes = Uint8Array.from(binario, (caracter) => caracter.charCodeAt(0))
    const payload = new TextDecoder().decode(bytes)

    return JSON.parse(payload)
  } catch {
    return null
  }
}
