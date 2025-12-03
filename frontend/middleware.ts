import { withAuth } from 'next-auth/middleware';
import { NextResponse } from 'next/server';

export default withAuth(
  function middleware(req) {
    // For authenticated users, continue.
    // Fine-grained authorization (owner/editor/super-admin) is checked in the layouts/pages.
    return NextResponse.next();
  },
  {
    callbacks: {
      authorized: ({ token }) => {
        // Require authentication for all /admin routes
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