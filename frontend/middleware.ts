import { NextResponse } from 'next/server';
import { getToken } from 'next-auth/jwt';

export default async function middleware(req) {
  const token = await getToken({ req });

  if (!token) {
    // If not authenticated, redirect to the public equivalent
    const url = req.nextUrl.clone();
    url.pathname = url.pathname.replace(/^\/admin/, '');
    url.searchParams.delete('callbackUrl'); // Remove any callbackUrl if present
    return NextResponse.redirect(url);
  }

  // For authenticated users, continue.
  return NextResponse.next();
}

export const config = {
  matcher: ['/admin/:path*'],
};