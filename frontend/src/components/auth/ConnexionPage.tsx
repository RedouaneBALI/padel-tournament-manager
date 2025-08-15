'use client';

import { signOut, useSession } from 'next-auth/react';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { signIn } from 'next-auth/react';

export default function ConnexionPage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (session && status === 'authenticated') {
      router.push('/admin/tournament/new');
    }
  }, [session, status, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="bg-card shadow-md rounded-lg px-10 py-8 text-center">
        <p className="mb-8 text-muted text-base">Veuillez vous reconnecter</p>

        {status === 'loading' ? (
          <p className="text-base text-muted mb-2">Chargement...</p>
        ) : session ? (
          <div className="flex flex-col items-center gap-4">
            <span className="text-base text-foreground">
              Connecté en tant que <span className="font-medium">{session.user?.email}</span>
            </span>
            <button
              onClick={() => signOut({ callbackUrl: '/connexion' })}
              className="px-5 py-2 text-base bg-primary text-on-primary rounded hover:bg-primary-hover transition"
            >
              Se déconnecter
            </button>
          </div>
        ) : (
          <div className="flex justify-center">
            <button
              onClick={() =>
                signIn('google', { redirect: true, callbackUrl: '/admin/tournament/new' })
              }
              className="flex items-center justify-center gap-2 px-5 py-2 text-base bg-card border border-border rounded hover:bg-background transition"
            >
              <img src="/google-logo.svg" alt="Google" className="w-5 h-5" />
              <span className="text-foreground">Se connecter avec Google</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
