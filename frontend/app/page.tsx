'use client';

import Link from 'next/link';
import { useSession } from 'next-auth/react';
import { signIn } from 'next-auth/react';

export default function Home() {
  const { status } = useSession();

  // Authenticated: show the main hero with action buttons
  return (
    <main className="min-h-screen bg-background">
      <section className="max-w-5xl mx-auto px-4 py-16 sm:py-24">
        <div className="bg-card border border-border rounded-2xl p-8 sm:p-12 shadow-sm">
          <div className="text-center space-y-6">
            <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-foreground">
              Padel Tournament Manager
            </h1>

            <p className="text-base sm:text-lg text-muted-foreground max-w-2xl mx-auto">
              Crée, organise et gère tes tournois de padel en quelques clics.
              Une interface simple pour préparer les tableaux, gérer les équipes et suivre les scores.
            </p>

            <div className="flex flex-wrap justify-center gap-3 sm:gap-4">
              {status === 'authenticated' ? (
                <>
                  <Link
                    href="/admin/tournament/new"
                    className="px-5 py-3 rounded-lg bg-primary text-on-primary font-semibold hover:bg-primary-hover transition"
                  >
                    Créer un tournoi
                  </Link>

                  <Link
                    href="/"
                    className="px-5 py-3 rounded-lg border border-border bg-background text-foreground hover:bg-card transition"
                  >
                    Mes tournois
                  </Link>
                </>
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

            <div className="grid sm:grid-cols-3 gap-4 mt-10 text-left">
              <div className="rounded-xl border border-border bg-background p-5">
                <h3 className="font-semibold text-foreground mb-1">Création rapide</h3>
                <p className="text-sm text-muted-foreground">
                  Renseigne quelques infos et démarre en moins d’une minute.
                </p>
              </div>

              <div className="rounded-xl border border-border bg-background p-5">
                <h3 className="font-semibold text-foreground mb-1">Paires & tirages</h3>
                <p className="text-sm text-muted-foreground">
                  Ajoute les équipes, configure les formats, génère le tirage automatiquement.
                </p>
              </div>

              <div className="rounded-xl border border-border bg-background p-5">
                <h3 className="font-semibold text-foreground mb-1">Gestion en direct</h3>
                <p className="text-sm text-muted-foreground">
                  Mets à jour les résultats et suis l’avancement du tournoi en temps réel.
                </p>
              </div>
            </div>

          </div>
        </div>
      </section>
    </main>
  );
}