'use client';

import { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { useSession } from 'next-auth/react';
import { fetchMyTournaments, deleteTournament } from '@/src/api/tournamentApi';
import type { Tournament } from '@/src/types/tournament';
import { toast } from 'react-toastify';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import TournamentCard from '@/src/components/tournament/TournamentCard';
import MultiSelectFilter from '@/src/components/frmt-ranking/filters/MultiSelectFilter';

export default function TournamentList() {
  const { status } = useSession();

  const [items, setItems] = useState<Tournament[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | number | null>(null);
  const [selectedLevels, setSelectedLevels] = useState<string[]>([]);
  const [selectedGenders, setSelectedGenders] = useState<string[]>([]);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      if (status !== 'authenticated') {
        if (!cancelled) setLoading(false);
        return;
      }
      if (!cancelled) setLoading(true);
      try {
        const data = await fetchMyTournaments('all');
        if (!cancelled) {
          setItems(data);
          // Initialize filters with all available options
          const levels = Array.from(new Set(data.map((t) => t.level || '')));
          const genders = Array.from(new Set(data.map((t) => t.gender || '')));
          setSelectedLevels([...levels]);
          setSelectedGenders([...genders]);
        }
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

  const availableLevels = useMemo(() => {
    if (!items) return [];
    const levelOrder = ['AMATEUR', 'P25', 'P50', 'P100', 'P250', 'P500', 'P1000', 'P1500', 'P2000'];
    const levels = Array.from(new Set(items.map((t) => t.level || '')));
    return levels.sort((a, b) => {
      if (a === '') return 1;
      if (b === '') return -1;
      const aIndex = levelOrder.indexOf(a);
      const bIndex = levelOrder.indexOf(b);
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
  }, [items]);

  const availableGenders = useMemo(() => {
    if (!items) return [];
    const genderOrder = ['MEN', 'WOMEN', 'MIX'];
    const genders = Array.from(new Set(items.map((t) => t.gender || '')));
    return genders.sort((a, b) => {
      if (a === '') return 1;
      if (b === '') return -1;
      const aIndex = genderOrder.indexOf(a);
      const bIndex = genderOrder.indexOf(b);
      if (aIndex === -1) return 1;
      if (bIndex === -1) return -1;
      return aIndex - bIndex;
    });
  }, [items]);

  const filteredItems = useMemo(() => {
    if (!items) return [];
    return items.filter((t) => {
      const levelMatch = selectedLevels.length === 0 || selectedLevels.includes(t.level || '');
      const genderMatch = selectedGenders.length === 0 || selectedGenders.includes(t.gender || '');
      return levelMatch && genderMatch;
    });
  }, [items, selectedLevels, selectedGenders]);

  const formatOption = (opt: string) => opt || '?';

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
              {(items?.length ?? 0) > 0 && (
                <div className="mb-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <MultiSelectFilter
                    label="Niveau"
                    availableOptions={availableLevels}
                    selectedOptions={selectedLevels}
                    onChange={setSelectedLevels}
                    allItems={items ?? []}
                    keyField="level"
                    renderOption={formatOption}
                  />
                  <MultiSelectFilter
                    label="Genre"
                    availableOptions={availableGenders}
                    selectedOptions={selectedGenders}
                    onChange={setSelectedGenders}
                    allItems={items ?? []}
                    keyField="gender"
                    renderOption={formatOption}
                  />
                </div>
              )}

              {(items?.length ?? 0) > 0 ? (
                <div>
                  {filteredItems.length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                      {filteredItems.map((t) => (
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
                      Aucun tournoi ne correspond à ces critères.
                    </div>
                  )}
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