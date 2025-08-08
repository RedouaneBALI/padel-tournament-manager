'use client';

import { signOut, useSession } from 'next-auth/react';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function ConnexionPage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (session && status === 'authenticated') {
      router.push('/admin/tournament/new');
    }
  }, [session, status, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="bg-white shadow-md rounded-lg px-10 py-8 text-center">
        <h1 className="text-3xl font-bold mb-5">Bienvenue</h1>
        <p className="mb-8 text-gray-600 text-base">Connectez-vous pour accéder à l&apos;administration</p>

        {status === 'loading' ? (
          <p className="text-base text-gray-500 mb-2">Chargement...</p>
        ) : session ? (
          <div className="flex flex-col items-center gap-4">
            <span className="text-base text-gray-700">
              Connecté en tant que <span className="font-medium">{session.user?.email}</span>
            </span>
            <button
              onClick={() => signOut()}
              className="px-5 py-2 text-base bg-gray-800 text-white rounded hover:bg-gray-700 transition"
            >
              Se déconnecter
            </button>
          </div>
        ) : (
          <div className="flex justify-center">
            <button
              onClick={() => { window.location.href = '/api/auth/signin/google'; }}
              className="flex items-center justify-center gap-2 px-5 py-2 text-base bg-white border border-gray-300 rounded hover:bg-gray-100 transition"
            >
              <img src="/google-logo.svg" alt="Google" className="w-5 h-5" />
              <span className="text-gray-800">Se connecter avec Google</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
