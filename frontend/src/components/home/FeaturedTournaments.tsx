'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { fetchActiveTournaments } from '@/src/api/tournamentApi';
import { formatDate, formatDateRange, levelEmoji, genderEmoji, genderLabel, filterActiveTournaments } from './tournamentHelpers';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

type FeaturedTournamentsProps = {
  items?: any[];
  loading?: boolean;
};

export default function FeaturedTournaments({ items, loading }: FeaturedTournamentsProps = {}) {
  // If parent provides items, use them; otherwise fetch locally.
  const [featured, setFeatured] = useState<any[]>(items ? filterActiveTournaments(items) : []);
  const [loadingFeatured, setLoadingFeatured] = useState<boolean>(loading ?? false);

  useEffect(() => {
    // If parent provided items, reflect changes and skip fetching
    if (items) {
      setFeatured(filterActiveTournaments(items));
      setLoadingFeatured(loading ?? false);
      return;
    }

    let mounted = true;
    setLoadingFeatured(true);
    fetchActiveTournaments()
      .then((list) => {
        if (mounted) setFeatured(filterActiveTournaments(list));
      })
      .catch(() => {})
      .finally(() => mounted && setLoadingFeatured(false));
    return () => {
      mounted = false;
    };
  }, [items, loading]);

  const visibleFeatured = featured.slice(0, 4);

  return (
    <div className="mb-6">
      <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-foreground mb-2">Tournois à la une</h2>

      {loadingFeatured ? (
        // simple centered loader while fetching — no blocks visible by default
        <div className="flex justify-center py-4">
          <CenteredLoader />
        </div>
      ) : (
        // render grid only when we have items
        featured && featured.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {visibleFeatured.map((t) => (
              <Link
                key={t.id}
                href={`/tournament/${t.id}/games`}
                aria-label={`Voir le tournoi ${t.name}`}
                className="block py-4 px-6 border border-border rounded-2xl bg-card shadow-md hover:shadow-lg transition-transform transform hover:-translate-y-1"
              >
                <div className="w-full max-w-[20rem] sm:max-w-none mx-auto">
                  <div className="flex flex-col items-center text-center">
                    <div className="text-sm text-muted-foreground">{`📍 ${t.city || ''}`}</div>
                    <div className="text-lg sm:text-xl font-extrabold text-foreground mt-1 whitespace-normal leading-snug">{t.name}</div>
                    {t.club && (
                      <div className="mt-1 text-sm text-muted-foreground whitespace-normal">{`🏟️ ${t.club}`}</div>
                    )}

                    <div className="mt-2 flex items-center gap-2 justify-center">
                      { (t.gender || t.level) && (
                        <>
                          {t.gender && (
                            <span className="inline-flex items-center gap-1 text-sm text-muted-foreground px-2 py-0.5 border border-border rounded-full">
                              <span>{genderEmoji(t.gender)}</span>
                              <span className="font-medium">{genderLabel(t.gender)}</span>
                            </span>
                          )}
                          {t.level && (
                            <span className="inline-flex items-center gap-1 text-sm text-muted-foreground px-2 py-0.5 border border-border rounded-full">
                              <span>{levelEmoji(t.level)}</span>
                              <span className="font-medium">{t.level}</span>
                            </span>
                          )}
                        </>
                      )}
                    </div>

                    <div className="mt-2 flex items-center justify-center gap-2">
                      <svg className="w-4 h-4 text-muted-foreground" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden>
                        <path d="M7 11V7" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                        <path d="M17 11V7" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                        <rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                      </svg>
                      <span className="text-sm text-muted-foreground">{formatDateRange(t.startDate, t.endDate)}</span>
                    </div>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        ) : null
      )}
    </div>
  );
}
