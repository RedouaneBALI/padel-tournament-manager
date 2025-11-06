'use client';

import { signOut, useSession } from 'next-auth/react';
import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import GoogleLoginButton from '@/src/components/ui/buttons/GoogleLoginButton';

const errorMessages: Record<string, string> = {
  OAuthCallback: "Une erreur s'est produite lors de la connexion avec Google. Veuillez réessayer.",
  OAuthSignin: "Erreur lors de l'initialisation de la connexion.",
  OAuthCreateAccount: "Impossible de créer un compte. Veuillez réessayer.",
  EmailCreateAccount: "Impossible de créer un compte avec cet email.",
  Callback: "Erreur lors du processus de connexion.",
  OAuthAccountNotLinked: "Ce compte est déjà associé à un autre fournisseur.",
  EmailSignin: "Erreur lors de l'envoi de l'email de connexion.",
  CredentialsSignin: "Identifiants incorrects.",
  SessionRequired: "Veuillez vous connecter pour accéder à cette page.",
  Default: "Une erreur inattendue s'est produite.",
};

export default function ConnexionPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const errorParam = searchParams?.get('error');
    if (errorParam) {
      setError(errorMessages[errorParam] || errorMessages.Default);
    }
  }, [searchParams]);

  useEffect(() => {
    if (session && status === 'authenticated') {
      router.push('/admin/tournaments');
    }
  }, [session, status, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <div className="bg-card shadow-md rounded-lg px-10 py-8 text-center max-w-md w-full mx-4">
        <p className="mb-8 text-muted text-base">Veuillez vous connecter</p>

        {error && (
          <div className="mb-6 p-4 bg-destructive/10 border border-destructive/20 rounded-lg">
            <p className="text-destructive text-sm">{error}</p>
          </div>
        )}

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
            <GoogleLoginButton callbackUrl="/admin/tournaments" />
          </div>
        )}
      </div>
    </div>
  );
}
