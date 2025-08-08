import { NextAuthOptions } from 'next-auth';
import GoogleProvider from 'next-auth/providers/google';

// Vérification de la présence du secret
const nextAuthSecret = process.env.NEXTAUTH_SECRET;
console.log("authOptions.ts: NEXTAUTH_SECRET AT BUILD:", process.env.NEXTAUTH_SECRET);
if (!nextAuthSecret) {
  if (process.env.NODE_ENV === 'production') {
    console.log("authOptions.ts: process:", process);
    console.log("authOptions.ts: process.env:", process.env);
    throw new Error('NEXTAUTH_SECRET is not defined in production!');
  } else {
    console.warn('NEXTAUTH_SECRET is not defined. Using fallback value for development only!');
  }
}

export const authOptions: NextAuthOptions = {
  providers: [
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID!,
      clientSecret: process.env.GOOGLE_CLIENT_SECRET!,
    }),
  ],
  secret: nextAuthSecret || 'dev-secret',
  session: {
    strategy: "jwt",
  },
  pages: {
    signIn: "/auth/signin",
  },
  callbacks: {
    async session({ session, token }) {
      if (session.user && token.sub) {
        session.user.id = token.sub;
      }
      return session;
    },
    async jwt({ token, user }) {
      if (user) {
        token.sub = user.id;
      }
      return token;
    },
  },
};