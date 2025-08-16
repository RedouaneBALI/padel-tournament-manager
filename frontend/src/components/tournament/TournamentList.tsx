'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useSession } from 'next-auth/react';
import { fetchMyTournaments, deleteTournament } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import { toast } from 'react-toastify';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import TournamentCard from '@/src/components/tournament/TournamentCard';

export default function TournamentList() {
  const { status } = useSession();

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

  // Loading session: show loader
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
        <section className="max-w-3xl mx-auto">
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
    <main className="min-h-screen bg-background mb-15">
      <section className="max-w-5xl mx-auto">
        <div className="bg-card border border-border rounded-2xl p-6 sm:p-8">
          <h1 className="text-2xl sm:text-3xl font-bold text-foreground mb-6">Mes tournois</h1>

          {loading && <CenteredLoader size={24} />}

          {!loading && error && (
            <div className="text-red-600">Une erreur est survenue : {error}</div>
          )}

          {!loading && !error && (
            <>
              {(items?.length ?? 0) > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                  {(items ?? []).map((t) => (
                    <TournamentCard
                      key={t.id}
                      tournament={t}
                      onDelete={onDelete}
                      isDeleting={String(deletingId) === String(t.id)}
                    />
                  ))}
                </div>
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
