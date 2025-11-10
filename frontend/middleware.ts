import { withAuth } from 'next-auth/middleware';
import { NextResponse } from 'next/server';

export default withAuth(
  function middleware(req) {
    const token = req.nextauth.token;
    const pathname = req.nextUrl.pathname;

    // Check if user is trying to access admin tournament routes
    const adminTournamentMatch = pathname.match(/^\/admin\/tournament\/(\d+)(\/.*)?$/);

    if (adminTournamentMatch && !token) {
      // User not authenticated: redirect to public read-only version
      const tournamentId = adminTournamentMatch[1];
      const subPath = adminTournamentMatch[2] || '';
      const publicUrl = `/tournament/${tournamentId}${subPath}`;

      return NextResponse.redirect(new URL(publicUrl, req.url));
    }

    // For authenticated users, let them through
    // Authorization (owner/editor/super-admin) is handled by the backend
    // and the layout will redirect to read-only if not authorized
    return NextResponse.next();
  },
  {
    callbacks: {
      authorized: ({ token, req }) => {
        // Allow access to /admin/tournament/* routes without authentication
        // (they'll be redirected to public version by the middleware above)
        const pathname = req.nextUrl.pathname;
        if (pathname.match(/^\/admin\/tournament\/\d+/)) {
          return true;
        }
        // For other admin routes, require authentication
        return !!token;
      },
    },
    pages: {
      signIn: '/',
    },
  }
);

export const config = {
  matcher: ['/admin/:path*'],
};