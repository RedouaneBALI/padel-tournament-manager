import type { NextAuthOptions } from "next-auth";
import GoogleProvider from "next-auth/providers/google";

export function getAuthOptions(): NextAuthOptions {
  const secret = process.env.NEXTAUTH_SECRET;
  const clientId = process.env.GOOGLE_CLIENT_ID;
  const clientSecret = process.env.GOOGLE_CLIENT_SECRET;

  return {
    secret,
    session: { strategy: "jwt" },
    providers: [
      GoogleProvider({
        clientId: clientId ?? "",
        clientSecret: clientSecret ?? "",
        authorization: {
          params: {
            scope: "openid email profile",
            prompt: "consent",
            access_type: "offline",
            response_type: "code",
          },
        },
      })
    ],
    callbacks: {
      async jwt({ token, account }) {
        // 1er login : Google renvoie id_token (JWT) -> on le range dans le token NextAuth
        if (account) {
          (token as any).idToken = (account as any).id_token;
          (token as any).accessToken = account.access_token;
          // debug minimal:
          console.log("[JWT CB] has id_token:", !!(account as any).id_token);
        }
        return token;
      },
      async session({ session, token }) {
        // On expose idToken dans la session côté client
        (session as any).idToken = (token as any).idToken;
        (session as any).accessToken = (token as any).accessToken;
        return session;
      },
    },
  };
}
