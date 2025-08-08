import NextAuth from "next-auth";
import { NextRequest, NextResponse } from "next/server";
import { getAuthOptions } from "@/src/lib/authOptions";

const handler = NextAuth(getAuthOptions());

// Wrapper pour contourner le problème Next.js 15 + NextAuth v4
export async function GET(request: NextRequest, context: any) {
  try {
    // Convertir NextRequest en Request standard pour NextAuth v4
    const url = new URL(request.url);
    const headers = new Headers();

    // Copier les headers nécessaires
    request.headers.forEach((value, key) => {
      headers.set(key, value);
    });

    const standardRequest = new Request(url, {
      method: 'GET',
      headers: headers,
    });

    const response = await handler(standardRequest, context);
    return response;
  } catch (error) {
    console.error('Auth GET error:', error);
    return NextResponse.json({ error: 'Authentication failed' }, { status: 500 });
  }
}

export async function POST(request: NextRequest, context: any) {
  try {
    // Pour POST, on doit aussi gérer le body
    const body = await request.text();
    const url = new URL(request.url);
    const headers = new Headers();

    request.headers.forEach((value, key) => {
      headers.set(key, value);
    });

    const standardRequest = new Request(url, {
      method: 'POST',
      headers: headers,
      body: body || undefined,
    });

    const response = await handler(standardRequest, context);
    return response;
  } catch (error) {
    console.error('Auth POST error:', error);
    return NextResponse.json({ error: 'Authentication failed' }, { status: 500 });
  }
}