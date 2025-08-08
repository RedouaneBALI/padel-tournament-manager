// src/api/fetchWithAuth.ts
import { getSession } from "next-auth/react";

function normalizeHeaders(init?: HeadersInit): Record<string, string> {
  const h: Record<string, string> = {};
  if (!init) return h;
  if (init instanceof Headers) init.forEach((v, k) => (h[k] = v));
  else if (Array.isArray(init)) for (const [k, v] of init) h[k] = v;
  else Object.assign(h, init);
  return h;
}

export async function fetchWithAuth(url: string, options: RequestInit = {}) {
  const session = await getSession();

  const headers = normalizeHeaders(options.headers);
  if (!headers["Content-Type"]) headers["Content-Type"] = "application/json";
  if (session?.idToken) headers["Authorization"] = `Bearer ${session.idToken}`;

  return fetch(url, { ...options, headers });
}
