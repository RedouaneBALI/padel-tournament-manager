// src/lib/authOptions.ts
import { type NextAuthOptions } from "next-auth";
import GoogleProvider from "next-auth/providers/google";

// On étend le type pour ajouter trustHost
type NextAuthOptionsWithTrustHost = NextAuthOptions & {
  trustHost?: boolean;
};

export function getAuthOptions(): NextAuthOptionsWithTrustHost {
  const secret = process.env.NEXTAUTH_SECRET;
  const clientId = process.env.GOOGLE_CLIENT_ID;
  const clientSecret = process.env.GOOGLE_CLIENT_SECRET;

  if (!secret || !clientId || !clientSecret) {
    console.error("Missing NEXTAUTH envs at runtime:", {
      NEXTAUTH_SECRET: !!secret,
      GOOGLE_CLIENT_ID: !!clientId,
      GOOGLE_CLIENT_SECRET: !!clientSecret,
    });
  }
  return {
    trustHost: true,
    secret,
    providers: [
      GoogleProvider({
        clientId: clientId ?? "",
        clientSecret: clientSecret ?? "",
      }),
    ],
  };
}
