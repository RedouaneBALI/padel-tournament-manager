// app/admin/games/page.tsx
'use client';

import React, { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import { Home } from 'lucide-react';
import { FiMoreHorizontal } from 'react-icons/fi';
import { fetchMyGames } from '@/src/api/tournamentApi';
import { ToastContainer, toast } from 'react-toastify';
import MatchResultCard from '@/src/components/ui/MatchResultCard';
import type { Game } from '@/src/types/game';

export default function MyGamesPage() {
  const pathname = usePathname() ?? '';
  const router = useRouter();
  const items: BottomNavItem[] = [
    { href: '/', label: 'Accueil', Icon: Home, isActive: (p) => p === '/' },
    { href: '#more', label: 'Plus', Icon: FiMoreHorizontal },
  ];

  const [games, setGames] = useState<Game[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [overrides, setOverrides] = useState<Record<string, any>>({});

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    fetchMyGames('mine')
      .then((res) => {
        if (!mounted) return;
        setGames(res || []);
      })
      .catch((err) => {
        console.error(err);
        toast.error('Erreur lors du chargement de vos matchs');
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, []);

  // Listen to local updates dispatched by MatchResultCard
  useEffect(() => {
    const handler = (e: Event) => {
      try {
        const detail = (e as CustomEvent).detail;
        if (!detail || !detail.gameId) return;
        setOverrides((prev) => ({ ...prev, [String(detail.gameId)]: detail.score }));
      } catch (err) {
        // ignore
      }
    };
    window.addEventListener('game-updated', handler as EventListener);
    return () => window.removeEventListener('game-updated', handler as EventListener);
  }, []);

  const totalMatches = games?.length ?? 0;
  const setsToWin = 2;

  const handleGameClick = (gameId: string) => {
    router.push(`/admin/game/${gameId}`);
  };

  return (
    <>
      <main className="min-h-screen bg-background">
        <section className="max-w-4xl mx-auto p-4">
          <h1 className="text-2xl font-semibold mb-4">Mes matchs</h1>

          {loading && <div>Chargement...</div>}

          {!loading && (!games || games.length === 0) && <div>Aucun match trouvé.</div>}

          {!loading && games && games.length > 0 && (
            <div className="flex flex-col gap-4 w-full items-center mb-4">
              {games.map((game, idx) => {
                const winnerIndex = game.finished
                  ? game.winnerSide === 'TEAM_A'
                    ? 0
                    : game.winnerSide === 'TEAM_B'
                      ? 1
                      : undefined
                  : undefined;

                const originalIndex = idx;

                return (
                  <div key={game.id} className="w-full max-w-xl flex justify-center">
                    <div
                      onClick={() => handleGameClick(String(game.id))}
                      className="cursor-pointer transition-transform hover:scale-[1.02] w-full flex justify-center"
                    >
                      <MatchResultCard
                        teamA={game.teamA}
                        teamB={game.teamB}
                        score={overrides[String(game.id)] ?? (game as any).score}
                        gameId={String(game.id)}
                        tournamentId={String((game as any).tournamentId ?? '')}
                        editable={false}
                        court={(game as any).court}
                        scheduledTime={(game as any).scheduledTime}
                        onInfoSaved={() => { /* no-op */ }}
                        onTimeChanged={() => { /* no-op */ }}
                        onGameUpdated={() => { /* no-op */ }}
                        winnerSide={winnerIndex}
                        stage={(game as any).stage}
                        pool={(game as any).pool}
                        setsToWin={setsToWin}
                        finished={game.finished}
                        matchIndex={originalIndex}
                        totalMatches={totalMatches}
                        isFirstRound={false}
                        updateGameFn={undefined}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>
      </main>
      <BottomNav items={items} pathname={pathname} />
      <ToastContainer />
    </>
  );
}
