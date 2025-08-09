import { DefaultSession } from "next-auth";

declare module "next-auth" {
  interface Session {
    idToken?: string;
    accessToken?: string;
    user?: DefaultSession["user"];
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    idToken?: string;
    accessToken?: string;
  }
}

export {};