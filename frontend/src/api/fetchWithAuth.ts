import { getSession, signOut } from "next-auth/react";

function normalizeHeaders(init?: HeadersInit): Record<string, string> {
  const h: Record<string, string> = {};
  if (!init) return h;
  if (init instanceof Headers) init.forEach((v, k) => (h[k] = v));
  else if (Array.isArray(init)) for (const [k, v] of init) h[k] = v;
  else Object.assign(h, init);
  return h;
}

export async function fetchWithAuth(url: string, options: RequestInit = {}) {
  let session = await getSession();

  const headers = normalizeHeaders(options.headers);
  if (!headers["Content-Type"] && (options.method && options.method !== 'GET')) {
    headers["Content-Type"] = "application/json";
  }

  const bearer = (session as any)?.accessToken ?? (session as any)?.idToken;
  if (!bearer) {
    console.warn("[fetchWithAuth] No token in session. Are you logged in?");
  } else {
    headers["Authorization"] = `Bearer ${bearer}`;
  }

  let res = await fetch(url, { ...options, headers });

  if (res.status === 401) {
    session = await getSession();
    const fresh = (session as any)?.accessToken ?? (session as any)?.idToken;
    if (fresh) {
      headers["Authorization"] = `Bearer ${fresh}`;
      res = await fetch(url, { ...options, headers });
    }
    if (res.status === 401) {
      try { await signOut({ callbackUrl: "/" }); } catch {}
    }
  }

  return res;
}