import { NextResponse, NextRequest } from 'next/server';
import { getToken } from 'next-auth/jwt';

export default async function middleware(req: NextRequest) {
  const token = await getToken({ req });
  const url = req.nextUrl.clone();

  // Public routes that don't require auth
  const isPublicRoute = url.pathname.startsWith('/connexion') ||
                        url.pathname.startsWith('/api/auth');

  if (!token && !isPublicRoute) {
    // If not authenticated and trying to access protected route, redirect to login
    if (url.pathname.startsWith('/admin')) {
      url.pathname = url.pathname.replace(/^\/admin/, '');
      return NextResponse.redirect(url);
    }
  }

  // Check if user has completed their profile
  if (token && url.pathname !== '/mon-compte' && !url.pathname.startsWith('/api')) {
    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
      const idToken = (token as any).idToken || (token as any).accessToken;

      const response = await fetch(`${baseUrl}/user/profile`, {
        headers: {
          'Authorization': `Bearer ${idToken}`,
        },
      });

      if (response.ok) {
        const profile = await response.json();

        // Redirect to profile completion if name or profileType is missing
        if (!profile.name || !profile.profileType) {
          url.pathname = '/mon-compte';
          url.searchParams.set('returnUrl', req.nextUrl.pathname);
          return NextResponse.redirect(url);
        }
      }
    } catch (error) {
      console.error('Error checking profile completion:', error);
      // Continue on error to avoid blocking access
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    '/admin/:path*',
    '/mon-compte',
    '/favorites/:path*',
    // Add other protected routes here
  ],
};

