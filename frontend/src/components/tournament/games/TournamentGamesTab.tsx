// TournamentGamesTab.tsx
'use client';

import { useEffect, useState, useCallback, useMemo } from 'react';
import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import RoundSelector from '@/src/components/round/RoundSelector';
import type { Round } from '@/src/types/round';
import type { Game } from '@/src/types/game';
import { Stage } from '@/src/types/stage';
import { fetchTournament } from '@/src/api/tournamentApi';
import GroupSelector from '@/src/components/tournament/games/GroupSelector';
import GamesList from '@/src/components/tournament/games/GamesList';
import { exportGamesAsCSV } from '@/src/utils/gamesExport';
import { useExport } from '@/src/contexts/ExportContext';

function updateGameInRounds(rounds: Round[], gameId: string, changes: { scheduledTime?: string; court?: string }): Round[] {
  return rounds.map((round) => ({
    ...round,
    games: round.games.map((g) =>
      String(g.id) === String(gameId)
        ? {
            ...g,
            ...(changes.scheduledTime !== undefined ? { scheduledTime: changes.scheduledTime } : {}),
            ...(changes.court !== undefined ? { court: changes.court } : {})
          }
        : g
    ),
  }));
}

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

const STAGE_VALUES = Object.values(Stage) as string[];
function isStage(value: unknown): value is Stage {
  return typeof value === 'string' && STAGE_VALUES.includes(value);
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
  const { setAdminActions } = useExport();

  const getValidGamesSortedByTime = useCallback((games: Game[]) => {
    return games
      .filter(game =>
        // On ne filtre plus sur teamA/teamB null, on garde seulement le filtre BYE
        (game.teamA?.player1Name !== 'BYE' &&
         game.teamA?.player2Name !== 'BYE' &&
         game.teamB?.player1Name !== 'BYE' &&
         game.teamB?.player2Name !== 'BYE')
      )
      .sort((a, b) => {
        if (!a.scheduledTime && !b.scheduledTime) return 0;
        if (!a.scheduledTime) return 1;
        if (!b.scheduledTime) return -1;
        return a.scheduledTime.localeCompare(b.scheduledTime);
      });
  }, []);

  const handleTimeChanged = useCallback((gameId: string, newTime: string) => {
    console.debug('[handleTimeChanged]', gameId, newTime);
    setRounds((currentRounds) => currentRounds.map((round) => ({
      ...round,
      games: round.games.map((g) => (String(g.id) === String(gameId) ? { ...g, scheduledTime: newTime } : g)),
    })));
  }, []);

  const handleInfoSaved = useCallback((result: { tournamentUpdated: boolean; winner: string | null }) => {
    if (result.tournamentUpdated) {
      fetchTournament(tournamentId)
        .then((t) => setRounds((t as any)?.rounds ?? []))
        .catch((error) => console.error('Erreur lors du rafraîchissement du tournoi :', error));
    }
  }, [tournamentId]);

  // Quand MatchResultCard notifie d'un changement de court/heure, mettre à jour l'état local
  const handleGameUpdated = useCallback((gameId: string, changes: { scheduledTime?: string; court?: string }) => {
    setRounds((currentRounds) => updateGameInRounds(currentRounds, gameId, changes));
  }, []);

  useEffect(() => {
    async function loadRounds() {
      try {
        setIsLoading(true);
        const t = await fetchTournament(tournamentId);
        const initialRounds: Round[] = (t as any)?.rounds ?? [];
        setRounds(initialRounds);
        if (initialRounds.length > 0) {
          const currentStageFromApi = (t as any)?.currentRoundStage as Stage | null;
          const desiredStage: Stage = (isStage(stageParam) ? stageParam : undefined)
            || currentStageFromApi
            || (initialRounds[0].stage as Stage);
          const finalIdx = Math.max(0, initialRounds.findIndex((r) => r.stage === desiredStage));
          setCurrentRoundIndex(finalIdx);
          if (!stageParam) {
            const sp = new URLSearchParams(searchParams?.toString?.() ?? '');
            sp.set('stage', initialRounds[finalIdx].stage as any);
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
    const desired = isStage(stageParam) ? (stageParam as Stage) : undefined;
    if (!desired) return;
    const idx = rounds.findIndex((r) => r.stage === desired);
    if (idx !== -1 && idx !== currentRoundIndex) {
      setCurrentRoundIndex(idx);
      setSelectedGroup('ALL');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stageParam, rounds]);

  const handleStageChangeInUrl = useCallback(
    (newStage: string | Stage) => {
      if (!newStage) return;
      const stageValue = isStage(newStage) ? newStage : (String(newStage) as string);
      const sp = new URLSearchParams(searchParams?.toString?.() ?? '');
      sp.set('stage', stageValue);
      router.replace(`${pathname}?${sp.toString()}`);
      const idx = rounds.findIndex((r) => r.stage === stageValue);
      if (idx !== -1) setCurrentRoundIndex(idx);
    },
    [router, pathname, searchParams, rounds]
  );

  const currentRound = rounds[currentRoundIndex];

  const sortedGames = useMemo(() => (
    currentRound ? getValidGamesSortedByTime(currentRound.games) : []
  ), [currentRound, getValidGamesSortedByTime]);

  const isGroupStage = useMemo(() => currentRound?.stage === Stage.GROUPS, [currentRound]);

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

  // Register export function with context - only set if we have games
  useEffect(() => {
    if (currentRound?.games && currentRound.games.length > 0) {
      setAdminActions({
        onExport: () => exportGamesAsCSV(currentRound.games, currentRound.stage)
      });
    }
    // Don't reset to null on unmount or when games become empty
    // The parent layout will handle the default state
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentRound?.id, currentRound?.stage, currentRound?.games?.length]);

  if (isLoading) {
    return <CenteredLoader />;
  }

  if (rounds.length === 0) {
    return <p className="text-muted">Aucun round défini pour le moment.</p>;
  }

  const setsToWin = getSetsToWin(currentRound);

  return (
    <div className="flex flex-col items-center space-y-4 mb-15">
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
        onGameUpdated={handleGameUpdated}
        stage={currentRound.stage as unknown as string}
        isFirstRound={currentRoundIndex === 0}
      />
    </div>
  );
}
