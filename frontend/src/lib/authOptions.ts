import type { NextAuthOptions } from "next-auth";
import GoogleProvider from "next-auth/providers/google";

export function getAuthOptions(): NextAuthOptions {
  const secret = process.env.NEXTAUTH_SECRET;
  const clientId = process.env.GOOGLE_CLIENT_ID;
  const clientSecret = process.env.GOOGLE_CLIENT_SECRET;

  return {
    secret,
      session: {
        strategy: 'jwt',
        maxAge: 365 * 24 * 60 * 60,   // 1 an
        updateAge: 24 * 60 * 60,      // on rafraîchit la session au moins 1x/jour
      },
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
    pages: {
      signIn: '/connexion',
      error: '/connexion', // Redirect errors to sign-in page
    },
    callbacks: {
      async jwt({ token, account }) {
        // 1er login : Google renvoie id_token (JWT) -> on le range dans le token NextAuth
        if (account) {
          (token as any).idToken = (account as any).id_token;
          (token as any).accessToken = account.access_token;
          (token as any).refreshToken = (account as any).refresh_token;
          (token as any).accessTokenExpires = (account as any).expires_at
            ? (account as any).expires_at * 1000
            : Date.now() + 3600 * 1000; // fallback 1 hour
          // debug minimal:
          console.log("[JWT CB] has id_token:", !!(account as any).id_token);
          return token;
        }

        // Return previous token if the access token has not expired yet
        if ((token as any).accessToken && (token as any).accessTokenExpires && Date.now() < (token as any).accessTokenExpires - 60_000) {
          return token;
        }

        // Access token has expired, try to update it
        return await refreshAccessToken(token);
      },
      async session({ session, token }) {
        // On expose idToken dans la session côté client
        (session as any).idToken = (token as any).idToken;
        (session as any).accessToken = (token as any).accessToken;
        (session as any).accessTokenExpires = (token as any).accessTokenExpires;
        (session as any).error = (token as any).error;
        return session;
      },
      async redirect({ url, baseUrl }) {
        // Permet les redirections vers le même domaine ou vers des URLs relatives
        if (url.startsWith("/")) return `${baseUrl}${url}`;
        // Permet les redirections vers le domaine de base
        else if (new URL(url).origin === baseUrl) return url;
        return baseUrl;
      },
    },
  };
}

async function refreshAccessToken(token: any) {
  try {
    const url = "https://oauth2.googleapis.com/token";

    const response = await fetch(url, {
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      method: "POST",
      body: new URLSearchParams({
        client_id: process.env.GOOGLE_CLIENT_ID ?? "",
        client_secret: process.env.GOOGLE_CLIENT_SECRET ?? "",
        grant_type: "refresh_token",
        refresh_token: token.refreshToken,
      }),
    });

    const refreshedTokens = await response.json();

    if (!response.ok) {
      throw refreshedTokens;
    }

    return {
      ...token,
      accessToken: refreshedTokens.access_token,
      idToken: refreshedTokens.id_token ?? token.idToken,
      accessTokenExpires: Date.now() + (refreshedTokens.expires_in ?? 3600) * 1000,
      refreshToken: refreshedTokens.refresh_token ?? token.refreshToken,
      error: undefined,
    };
  } catch (error) {
    console.error("Error refreshing access token", error);

    return {
      ...token,
      error: "RefreshAccessTokenError",
    };
  }
}
