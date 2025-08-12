'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useSession } from 'next-auth/react';
import { fetchMyTournaments } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';

export default function TournamentsPage() {
  const { status } = useSession();
  const [items, setItems] = useState<Tournament[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      if (status !== 'authenticated') {
        setLoading(false);
        return;
      }
      try {
        const data = await fetchMyTournaments('mine');
        if (!cancelled) setItems(data);
      } catch (e: any) {
        if (!cancelled) setError(e?.message || 'Erreur inconnue');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [status]);

  // Not authenticated: show a simple CTA to sign in with Google
  if (status !== 'authenticated') {
    return (
      <main className="min-h-screen bg-background">
        <section className="max-w-3xl mx-auto px-4 py-16">
          <div className="bg-card border border-border rounded-2xl p-8 text-center">
            <h1 className="text-2xl font-bold text-foreground mb-2">Mes tournois</h1>
            <p className="text-muted-foreground mb-6">Connecte-toi pour voir tes tournois.</p>
            <Link
              href="/api/auth/signin/google?callbackUrl=/tournaments"
              className="flex items-center justify-center gap-2 border border-border bg-card hover:bg-background transition px-5 py-3 rounded-lg font-medium mx-auto w-fit"
            >
              <img src="/google-logo.svg" alt="" className="w-5 h-5" />
              Se connecter avec Google
            </Link>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-background">
      <section className="max-w-5xl mx-auto px-4 py-16">
        <div className="bg-card border border-border rounded-2xl p-6 sm:p-8">
          <h1 className="text-2xl sm:text-3xl font-bold text-foreground mb-6">Mes tournois</h1>

          {loading && <div className="text-muted-foreground">Chargement…</div>}

          {!loading && error && (
            <div className="text-red-600">Une erreur est survenue : {error}</div>
          )}

          {!loading && !error && (
            <>
              {items && items.length > 0 ? (
                <ul className="divide-y divide-border">
                  {items.map((t) => (
                    <li key={t.id} className="py-3 flex items-center justify-between">
                      <div>
                        <div className="font-semibold text-foreground">{t.name || 'Sans nom'}</div>
                      </div>
                      <div className="flex gap-2">
                        <Link
                          href={`/admin/tournament/${t.id}`}
                          className="px-3 py-2 rounded-md bg-primary text-on-primary hover:bg-primary-hover transition text-sm"
                        >
                          Gérer
                        </Link>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="text-muted-foreground">
                  Aucun tournoi pour le moment.
                  <Link href="/admin/tournament/new" className="ml-2 underline">
                    Créer un tournoi
                  </Link>
                </div>
              )}
            </>
          )}
        </div>
      </section>
    </main>
  );
}

// rm -rf .firebase/functions/.next .next && npm run build:functions && firebase deploy --only functions
