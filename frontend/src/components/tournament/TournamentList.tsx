'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useSession } from 'next-auth/react';
import { fetchMyTournaments, deleteTournament } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import { toast } from 'react-toastify';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

export default function TournamentList() {
  const { status } = useSession();
  if (status === 'loading') {
    return (
      <main className="min-h-screen bg-background">
        <section className="max-w-5xl mx-auto px-4 py-16">
          <div className="bg-card border border-border rounded-2xl p-6 sm:p-8">
            <CenteredLoader />
          </div>
        </section>
      </main>
    );
  }
  const [items, setItems] = useState<Tournament[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | number | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      if (status !== 'authenticated') {
        if (!cancelled) setLoading(false);
        return;
      }
      if (!cancelled) setLoading(true);
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

  async function onDelete(id: string | number) {
    if (!confirm('Supprimer ce tournoi ? Cette action est irréversible.')) return;
    try {
      setDeletingId(id);
      await deleteTournament(id);
      setItems((prev) => (prev ? prev.filter((t) => String(t.id) !== String(id)) : prev));
      toast.success('Tournoi supprimé.');
    } catch (e: any) {
      toast.error(e?.message || 'Suppression impossible.');
    } finally {
      setDeletingId(null);
    }
  }

  // Not authenticated: CTA login
  if (status !== 'authenticated') {
    return (
      <main className="min-h-screen bg-background">
        <section className="max-w-3xl mx-auto px-4 py-16">
          <div className="bg-card border border-border rounded-2xl p-8 text-center">
            <h1 className="text-2xl font-bold text-foreground mb-2">Mes tournois</h1>
            <p className="text-muted-foreground mb-6">Connecte-toi pour voir tes tournois.</p>
            <Link
              href="/api/auth/signin/google?callbackUrl=/admin/tournaments"
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

          {loading && <CenteredLoader size={24} />}

          {!loading && error && (
            <div className="text-red-600">Une erreur est survenue : {error}</div>
          )}

          {!loading && !error && (
            <>
              {(items?.length ?? 0) > 0 ? (
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

                        <button
                          type="button"
                          onClick={() => onDelete(t.id!)}
                          disabled={String(deletingId) === String(t.id)}
                          aria-label="Supprimer le tournoi"
                          className="px-3 py-2 rounded-md border border-red-600 text-red-600 hover:bg-red-600/10 transition text-sm flex items-center gap-2 disabled:opacity-60"
                          title="Supprimer"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4">
                            <path d="M9 3h6a1 1 0 0 1 1 1v1h4v2h-1v12a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V7H4V5h4V4a1 1 0 0 1 1-1Zm1 2v0h4V5h-4Zm-1 6h2v7H9v-7Zm6 0h-2v7h2v-7Z"/>
                          </svg>
                          {String(deletingId) === String(t.id) ? 'Suppression…' : 'Supprimer'}
                        </button>
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
