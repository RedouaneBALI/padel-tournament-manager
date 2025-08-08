import type { NextApiRequest, NextApiResponse } from "next";
import NextAuth from "next-auth";
import { getAuthOptions } from "@/src/lib/authOptions";

export default function auth(req: NextApiRequest, res: NextApiResponse) {
  return (NextAuth as any)(req, res, getAuthOptions());
}
