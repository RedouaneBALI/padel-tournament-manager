import NextAuth from "next-auth";
import { getAuthOptions } from "@/src/lib/authOptions";

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const handler = NextAuth(getAuthOptions());
export { handler as GET, handler as POST };
