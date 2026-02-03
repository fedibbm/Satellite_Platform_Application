import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

export function middleware(request: NextRequest) {
  // Get the access token from cookies
  const token = request.cookies.get('accessToken')?.value
  const isAuthenticated = !!token
  
  // Clone the request headers
  const requestHeaders = new Headers(request.headers)
  
  // Add custom header with auth status for the page to read
  requestHeaders.set('x-user-authenticated', isAuthenticated ? 'true' : 'false')
  
  // Continue with the modified headers
  return NextResponse.next({
    request: {
      headers: requestHeaders,
    },
  })
}

// Run middleware on all pages
export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     */
    '/((?!_next/static|_next/image|favicon.ico).*)',
  ],
}
