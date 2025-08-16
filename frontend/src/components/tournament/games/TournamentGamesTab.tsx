'use client';

import { useEffect, useState, useCallback, useMemo } from 'react';
import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import RoundSelector from '@/src/components/round/RoundSelector';
import type { Round } from '@/src/types/round';
import type { Game } from '@/src/types/game';
import { fetchRounds } from '@/src/api/tournamentApi';
import GroupSelector from '@/src/components/tournament/games/GroupSelector';
import GamesList from '@/src/components/tournament/games/GamesList';

interface TournamentGamesTabProps {
  tournamentId: string;
  editable: boolean;
}

function getSetsToWin(round: any) {
  const n = Number(round?.matchFormat?.numberOfSetsToWin);
  return Number.isFinite(n) && n > 0 ? n : 2;
}

function normalizeGroup(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  let s = v.trim();
  if (!s) return null;
  s = s.replace(/^\s*(grou?pe|pool|poule)\s*/i, '');
  const parts = s.split(/\s+/);
  s = parts[parts.length - 1];
  return s.toUpperCase();
}

function pickGroupValue(obj: any): string | null {
  const candidate = obj?.name ?? obj?.label ?? obj?.code ?? obj?.id ?? obj?.letter ?? obj;
  return normalizeGroup(candidate);
}

export default function TournamentGamesTab({ tournamentId, editable }: TournamentGamesTabProps) {
  const [rounds, setRounds] = useState<Round[]>([]);
  const [currentRoundIndex, setCurrentRoundIndex] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedGroup, setSelectedGroup] = useState<'ALL' | string>('ALL');

  const router = useRouter();
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const stageParam = searchParams?.get('stage') ?? null;

  const getValidGamesSortedByTime = useCallback((games: Game[]) => {
    return games
      .filter(game =>
        (game.teamA !== null || game.teamB !== null) &&
        (game.teamA?.player1?.name !== 'BYE' &&
         game.teamA?.player2?.name !== 'BYE' &&
         game.teamB?.player1?.name !== 'BYE' &&
         game.teamB?.player2?.name !== 'BYE')
      )
      .sort((a, b) => {
        if (!a.scheduledTime && !b.scheduledTime) return 0;
        if (!a.scheduledTime) return 1;
        if (!b.scheduledTime) return -1;
        return a.scheduledTime.localeCompare(b.scheduledTime);
      });
  }, []);

  const handleTimeChanged = useCallback((gameId: string, newTime: string) => {
    setRounds((currentRounds) => currentRounds.map((round) => ({
      ...round,
      games: round.games.map((g) => (g.id === gameId ? { ...g, scheduledTime: newTime } : g)),
    })));
  }, []);

  const handleInfoSaved = useCallback((result: { tournamentUpdated: boolean; winner: string | null }) => {
    if (result.tournamentUpdated) {
      fetchRounds(tournamentId)
        .then((newRounds) => setRounds(newRounds))
        .catch((error) => console.error('Erreur lors du rafraîchissement des rounds :', error));
    }
  }, [tournamentId]);

  useEffect(() => {
    async function loadRounds() {
      try {
        setIsLoading(true);
        const initialRounds = await fetchRounds(tournamentId);
        setRounds(initialRounds);
        if (initialRounds.length > 0) {
          const desiredStage = stageParam || initialRounds[0].stage;
          const idx = initialRounds.findIndex((r) => r.stage === desiredStage);
          setCurrentRoundIndex(idx >= 0 ? idx : 0);
          if (!stageParam) {
            const sp = new URLSearchParams(searchParams?.toString?.() ?? '');
            sp.set('stage', desiredStage);
            router.replace(`${pathname}?${sp.toString()}`);
          }
        } else {
          setCurrentRoundIndex(0);
        }
      } catch (err) {
        console.error('Erreur lors du chargement des matchs :', err);
      } finally {
        setIsLoading(false);
      }
    }
    loadRounds();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tournamentId]);

  useEffect(() => {
    if (!rounds.length || !stageParam) return;
    const idx = rounds.findIndex((r) => r.stage === stageParam);
    if (idx !== -1 && idx !== currentRoundIndex) {
      setCurrentRoundIndex(idx);
      setSelectedGroup('ALL');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stageParam, rounds]);

  const handleStageChangeInUrl = useCallback(
    (newStage: string) => {
      if (!newStage) return;
      const sp = new URLSearchParams(searchParams?.toString?.() ?? '');
      sp.set('stage', newStage);
      router.replace(`${pathname}?${sp.toString()}`);
      const idx = rounds.findIndex((r) => r.stage === newStage);
      if (idx !== -1) setCurrentRoundIndex(idx);
    },
    [router, pathname, searchParams, rounds]
  );

  const currentRound = rounds[currentRoundIndex];

  const sortedGames = useMemo(() => (
    currentRound ? getValidGamesSortedByTime(currentRound.games) : []
  ), [currentRound, getValidGamesSortedByTime]);

  const isGroupStage = useMemo(() => (
    currentRound?.stage?.toString?.().toUpperCase?.() === 'GROUPS' ||
    currentRound?.stage?.toString?.().toUpperCase?.() === 'GROUP_STAGE'
  ), [currentRound]);

  const availableGroups = useMemo(() => {
    const poolsAny: any[] = Array.isArray((currentRound as any)?.pools)
      ? (currentRound as any).pools
      : Array.isArray((currentRound as any)?.groups)
      ? (currentRound as any).groups
      : [];

    const roundLevel = poolsAny
      .map(pickGroupValue)
      .filter((g): g is string => !!g);

    const gameLevel = (currentRound?.games || [])
      .map((g) => pickGroupValue((g as any).pool ?? (g as any).group ?? (g as any).poule))
      .filter((g): g is string => !!g);

    return Array.from(new Set<string>([...roundLevel, ...gameLevel])).sort((a, b) =>
      a.localeCompare(b, 'fr')
    );
  }, [currentRound]);

  // Ensure selectedGroup stays valid when availableGroups change
  useEffect(() => {
    if (selectedGroup !== 'ALL' && !availableGroups.includes(selectedGroup)) {
      setSelectedGroup('ALL');
    }
  }, [availableGroups, selectedGroup]);

  const displayedGames = useMemo(() => (
    selectedGroup === 'ALL'
      ? sortedGames
      : sortedGames.filter(
          (g) => pickGroupValue((g as any).pool ?? (g as any).group ?? (g as any).poule) === selectedGroup
        )
  ), [sortedGames, selectedGroup]);

  if (isLoading) {
    return <CenteredLoader />;
  }

  if (rounds.length === 0) {
    return <p className="text-muted">Aucun round défini pour le moment.</p>;
  }

  const setsToWin = getSetsToWin(currentRound);

  return (
    <div className="flex flex-col items-center space-y-6 mb-15">
      <RoundSelector
        rounds={rounds}
        currentIndex={currentRoundIndex}
        onChange={(idx) => {
          setCurrentRoundIndex(idx);
          const stage = rounds[idx]?.stage;
          if (stage) handleStageChangeInUrl(stage);
        }}
        onStageChange={handleStageChangeInUrl}
      />

      {isGroupStage && (
        <GroupSelector
          groups={availableGroups}
          value={selectedGroup}
          onChange={setSelectedGroup}
        />
      )}

      <GamesList
        games={displayedGames}
        tournamentId={tournamentId}
        editable={editable}
        setsToWin={setsToWin}
        onInfoSaved={handleInfoSaved}
        onTimeChanged={handleTimeChanged}
        stage={currentRound.stage as unknown as string}
      />
    </div>
  );
}