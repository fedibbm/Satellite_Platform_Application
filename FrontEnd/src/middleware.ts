import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

function decodeJwtPayload(token: string): any {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null
    const payload = parts[1]
    const decoded = atob(payload)
    return JSON.parse(decoded)
  } catch {
    return null
  }
}

export function middleware(request: NextRequest) {
  const token = request.cookies.get('accessToken')?.value
  const isAuthenticated = !!token
  
  const requestHeaders = new Headers(request.headers)
  requestHeaders.set('x-user-authenticated', isAuthenticated ? 'true' : 'false')
  
  // Protect /admin routes: only users with ADMIN role can access
  if (request.nextUrl.pathname.startsWith('/admin')) {
    if (!isAuthenticated) {
      return NextResponse.redirect(new URL('/auth/login', request.url))
    }
    
    const decoded = decodeJwtPayload(token!)
    const roles: string[] = decoded?.roles || []
    const isAdmin = roles.some((role: string) => role.includes('ADMIN'))
    
    if (!isAdmin) {
      return NextResponse.redirect(new URL('/dashboard', request.url))
    }
  }
  
  return NextResponse.next({
    request: {
      headers: requestHeaders,
    },
  })
}

export const config = {
  matcher: [
    '/((?!_next/static|_next/image|favicon.ico).*)',
  ],
}
