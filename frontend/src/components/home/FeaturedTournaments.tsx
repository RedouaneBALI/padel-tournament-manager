'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { fetchActiveTournaments } from '@/src/api/tournamentApi';

export default function FeaturedTournaments() {
  const [featured, setFeatured] = useState<any[]>([]);
  const [loadingFeatured, setLoadingFeatured] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoadingFeatured(true);
    fetchActiveTournaments()
      .then((list) => {
        if (mounted) setFeatured(list || []);
      })
      .catch(() => {})
      .finally(() => mounted && setLoadingFeatured(false));
    return () => {
      mounted = false;
    };
  }, []);

  const formatDate = (d?: string) => {
    if (!d) return '';
    try {
      return new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(d));
    } catch (e) {
      return d || '';
    }
  };

  const formatDateRange = (start?: string, end?: string) => {
    if (!start && !end) return '';
    if (!start) return formatDate(end);
    if (!end) return formatDate(start);

    const ds = new Date(start);
    const de = new Date(end);
    if (isNaN(ds.getTime()) || isNaN(de.getTime())) return `${formatDate(start)} — ${formatDate(end)}`;

    const sameMonth = ds.getMonth() === de.getMonth() && ds.getFullYear() === de.getFullYear();
    const sameYear = ds.getFullYear() === de.getFullYear();

    if (sameMonth) {
      return `${ds.getDate()}–${de.getDate()} ${new Intl.DateTimeFormat('fr-FR', { month: 'short', year: 'numeric' }).format(de)}`;
    }
    if (sameYear) {
      return `${new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(ds)} — ${new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }).format(de)}`;
    }
    return `${formatDate(start)} — ${formatDate(end)}`;
  };

  // level fixed to 🏅
  const levelEmoji = (_l?: string) => '🏅';
  const genderEmoji = (g?: string) => {
    if (!g) return '⚧';
    const s = String(g).toUpperCase();
    if (s === 'MEN' || s === 'M' || s === 'MALE') return '♂️';
    if (s === 'WOMEN' || s === 'F' || s === 'FEMALE') return '♀️';
    if (s.includes('MIX') || s.includes('MIXED')) return '⚥';
    return '⚧';
  };
  const genderLabel = (g?: string) => {
    if (!g) return '';
    const s = String(g).toUpperCase();
    if (s === 'MEN') return 'Hommes';
    if (s === 'WOMEN') return 'Femmes';
    return s.charAt(0) + s.slice(1).toLowerCase();
  };

  const visibleFeatured = featured.slice(0, 4);

  return (
    <>
      {(loadingFeatured || (featured && featured.length > 0)) && (
        <div className="mb-6">
          <h2 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-foreground mb-2">Tournois à la une</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {loadingFeatured ? (
              [0, 1, 2].map((i) => (
                <div key={i} className="py-3 px-4 border border-border rounded-2xl bg-card">
                  <div className="w-full max-w-[20rem] sm:max-w-none mx-auto">
                    <div className="flex flex-col items-center text-center">
                      <div className="h-3 w-24 rounded bg-slate-200/30 dark:bg-slate-700/30 mb-2 animate-pulse" />
                      <div className="h-5 w-40 rounded bg-slate-200/30 dark:bg-slate-700/30 mb-2 animate-pulse" />
                      <div className="h-3 w-32 rounded bg-slate-200/30 dark:bg-slate-700/30 mb-2 animate-pulse" />

                      <div className="flex gap-2 justify-center mt-1">
                        <div className="h-6 w-14 rounded-full bg-slate-200/30 dark:bg-slate-700/30 animate-pulse" />
                        <div className="h-6 w-12 rounded-full bg-slate-200/30 dark:bg-slate-700/30 animate-pulse" />
                      </div>
                      <div className="h-4 w-28 rounded bg-slate-200/30 dark:bg-slate-700/30 mt-3 animate-pulse" />
                    </div>
                  </div>
                </div>
              ))
            ) : (
              visibleFeatured.map((t) => (
                <Link
                  key={t.id}
                  href={`/tournament/${t.id}`}
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
              ))
            )}
          </div>
        </div>
      )}
    </>
  );
}
