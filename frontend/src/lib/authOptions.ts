import type { NextAuthOptions } from "next-auth";
import GoogleProvider from "next-auth/providers/google";
import FacebookProvider from "next-auth/providers/facebook";

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

export function getAuthOptions(): NextAuthOptions {
  const secret = process.env.NEXTAUTH_SECRET;

  return {
    secret,
    debug: true, // IMPORTANT: Cela affichera l'erreur exacte dans votre terminal VS Code
    session: {
      strategy: 'jwt',
      maxAge: 365 * 24 * 60 * 60,
      updateAge: 24 * 60 * 60,
    },
    providers: [
      GoogleProvider({
        clientId: process.env.GOOGLE_CLIENT_ID ?? "",
        clientSecret: process.env.GOOGLE_CLIENT_SECRET ?? "",
        authorization: {
          params: {
            scope: "openid email profile",
            prompt: "consent",
            access_type: "offline",
            response_type: "code",
          },
        },
      }),
      FacebookProvider({
        clientId: process.env.FACEBOOK_CLIENT_ID ?? "",
        clientSecret: process.env.FACEBOOK_CLIENT_SECRET ?? "",
        authorization: {
          params: {
            scope: "email,public_profile",
          },
        },
      }),
    ],
    pages: {
      signIn: '/connexion',
      error: '/connexion',
    },
    callbacks: {
      async signIn({ user, account }) {
        // Create user in backend on first sign-in if not exists
        if (account && user.email) {
          try {
            const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080';
            const response = await fetch(`${baseUrl}/auth/signin`, {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${(account as any).id_token || account.access_token}`,
              },
              body: JSON.stringify({
                email: user.email,
                name: user.name || null,
                image: user.image || null,
                provider: account.provider,
              }),
            });

            if (!response.ok && response.status !== 409) {
              console.error('Failed to create/update user in backend:', await response.text());
              // Continue anyway to avoid blocking sign-in
            }
          } catch (error) {
            console.error('Error during backend user creation:', error);
            // Continue anyway to avoid blocking sign-in
          }
        }
        return true;
      },
      async jwt({ token, account, user }) {
        // Initial sign in
        if (account && user) {
          return {
            ...token,
            accessToken: account.access_token,
            accessTokenExpires: account.expires_at ? account.expires_at * 1000 : Date.now() + 3600 * 1000,
            refreshToken: account.refresh_token,
            provider: account.provider,
            idToken: (account as any).id_token,
          };
        }

        // Return previous token if the access token has not expired yet
        if ((token as any).accessTokenExpires && Date.now() < (token as any).accessTokenExpires - 60_000) {
          return token;
        }

        // Facebook tokens generally don't rotate securely via API like Google, return as is
        if ((token as any).provider === 'facebook') {
          return token;
        }

        return await refreshAccessToken(token);
      },
      async session({ session, token }) {
        (session as any).accessToken = (token as any).accessToken;
        (session as any).idToken = (token as any).idToken;
        (session as any).error = (token as any).error;

        // Assure que l'image et l'email remontent bien
        if (session.user) {
            session.user.image = token.picture as string | null | undefined;
            session.user.email = token.email as string | null | undefined;
            session.user.name = token.name as string | null | undefined;
        }

        return session;
      },
      async redirect({ url, baseUrl }) {
        // Handle auth callback redirects
        if (url.startsWith("/auth/check-profile")) {
          return `${baseUrl}/auth/check-profile`;
        }
        if (url.startsWith("/")) return `${baseUrl}${url}`;
        else if (new URL(url).origin === baseUrl) return url;
        return baseUrl;
      },
    },
  };
}