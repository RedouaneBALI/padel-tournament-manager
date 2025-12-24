'use client';

import React, { useState, useRef, useContext } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { Score } from '@/src/types/score';
import TeamScoreRow from '@/src/components/ui/TeamScoreRow';
import { normalizeGroup, groupBadgeClasses } from '@/src/utils/groupBadge';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import LiveMatchIndicator from '@/src/components/ui/LiveMatchIndicator';
import { initializeScoresFromScore } from '@/src/utils/scoreUtils';
import { computeVisibleSets, computeIsInProgress, computeBadgeLabel, computeShowChampion } from '@/src/utils/matchResultUtils';
import { useScoreSyncing } from '@/src/hooks/useScoreSyncing';
import { useSaveLogic } from '@/src/hooks/useSaveLogic';
import MatchHeader from '@/src/components/ui/MatchHeader';
import MatchFooter from '@/src/components/ui/MatchFooter';
import { TournamentContext } from '@/src/contexts/TournamentContext';
import { Share } from 'lucide-react';
import { toast } from 'react-toastify';
import MatchShareCard from '@/src/components/match/MatchShareCard';
import { Game } from '@/src/types/game';
import { MatchFormat } from '@/src/types/matchFormat';
import { shareMatchImage } from '@/src/utils/imageExport';
import { TeamSide } from '@/src/types/teamSide';
import { useFavorites } from '@/src/hooks/useFavorites';
import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';

interface Props {
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
  editable?: boolean;
  gameId: string;
  tournamentId: string;
  score?: Score;
  onInfoSaved?: (result: { tournamentUpdated: boolean; winner: TeamSide | null }) => void;
  onTimeChanged?: (gameId: string, newTime: string) => void;
  onGameUpdated?: (gameId: string, changes: { scheduledTime?: string; court?: string }) => void;
  winnerSide?: TeamSide;
  stage?: string;
  court?: string;
  scheduledTime?: string;
  pool?: { name?: string };
  setsToWin?: number;
  finished?: boolean;
  matchIndex?: number;
  totalMatches?: number;
  isFirstRound?: boolean;
  matchFormat?: any;
  updateGameFn?: (gameId: string, scorePayload: Score, court: string, scheduledTime: string) => Promise<any>;
  isFavorite?: boolean;
  onToggleFavorite?: (gameId: number, isFavorite: boolean) => void;
}

export default function MatchResultCard({
  teamA,
  teamB,
  editable = false,
  gameId,
  score,
  tournamentId,
  onInfoSaved,
  onTimeChanged,
  onGameUpdated,
  winnerSide,
  stage,
  court,
  scheduledTime,
  pool,
  setsToWin,
  finished = true,
  matchIndex,
  totalMatches,
  isFirstRound = false,
  matchFormat,
  updateGameFn,
  isFavorite: isFavoriteProp,
  onToggleFavorite: onToggleFavoriteProp,
}: Props) {
  const group = normalizeGroup(pool?.name);
  const contextTournament = useContext(TournamentContext);

  const [localCourt, setLocalCourt] = useState(court || 'Court central');
  const [localScheduledTime, setLocalScheduledTime] = useState(scheduledTime || '00:00');
  const [editing, setEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Keep local court/time in sync with props when not editing
  React.useEffect(() => {
    if (!editing) {
      setLocalCourt(court || 'Court central');
      setLocalScheduledTime(scheduledTime || '00:00');
    }
  }, [court, scheduledTime, editing]);

  const [isForfeit, setIsForfeit] = useState(score?.forfeit || false);
  const [forfeitedBy, setForfeitedBy] = useState<TeamSide | null>(score?.forfeitedBy || null);

  const [scores, setScores] = useState<string[][]>(() => initializeScoresFromScore(score));
  const [initialScores, setInitialScores] = useState<string[][]>(() => initializeScoresFromScore(score));

  // Local winnerSide to update immediately after saving
  const [localWinnerSide, setLocalWinnerSide] = useState<TeamSide | undefined>(winnerSide);

  // Local finished status to update immediately after saving (for LIVE indicator)
  const [localFinished, setLocalFinished] = useState(finished);

  // Update localWinnerSide and localFinished when props change (but not during editing to avoid clobbering)
  React.useEffect(() => {
    if (!editing) {
      setLocalWinnerSide(winnerSide);
      setLocalFinished(finished);
    }
  }, [winnerSide, finished, editing]);

  // Resync scores when `score` prop is updated from parent (e.g. polling)
  // Only resync when the serialized score actually changed to avoid clobbering local edits
  useScoreSyncing(score, editing, setScores, setInitialScores, setIsForfeit, setForfeitedBy);

  const inputRefs = useRef<(HTMLInputElement | null)[][]>(
    Array.from({ length: 2 }, () => Array(3).fill(null))
  );

  // Helper to compute visibleSets based on setsToWin and first two sets
  const visibleSets = computeVisibleSets(setsToWin, scores);

  const { handleSave } = useSaveLogic({
    isSaving,
    setIsSaving,
    scores,
    visibleSets,
    isForfeit,
    forfeitedBy,
    gameId,
    tournamentId,
    localCourt,
    localScheduledTime,
    updateGameFn,
    onInfoSaved,
    onTimeChanged,
    onGameUpdated,
    setScores,
    setInitialScores,
    setEditing,
    setIsForfeit,
    setForfeitedBy,
    setLocalCourt,
    setLocalScheduledTime,
    court,
    scheduledTime,
    isFirstRound,
    matchIndex,
    setLocalWinnerSide,
    setLocalFinished,
    matchFormat,
  });

  const handleKeyDown = (e: React.KeyboardEvent<Element>, teamIndex: number, setIndex: number) => {
    if (e.key === 'Enter' || e.key === 'NumpadEnter') {
      e.preventDefault();
      void handleSave();
    }
  };

  // Calculer si le match est en cours
  const isInProgress = computeIsInProgress(localFinished, score, scores);

  // Calculer le contenu du badge (garder la logique en dehors du JSX pour satisfaire TypeScript)
  const badgeLabel = computeBadgeLabel(pool, matchIndex, totalMatches);

  // Compute winner sides to avoid nested ternaries
  const teamAWinnerSide = localWinnerSide;
  const teamBWinnerSide = localWinnerSide;

  const onCancel = () => {
    setScores([...initialScores]);
    setLocalCourt(court || 'Court central');
    setLocalScheduledTime(scheduledTime || '00:00');
    setIsForfeit(score?.forfeit || false);
    setForfeitedBy(score?.forfeitedBy || null);
    setEditing(false);
  };

  const onEdit = () => {
    setEditing(true);
  };

  const showChampionA = computeShowChampion(stage, localFinished, localWinnerSide, 0);
  const showChampionB = computeShowChampion(stage, localFinished, localWinnerSide, 1);

  const onToggleForfeitA = () => {
    if (isForfeit && forfeitedBy === 'TEAM_A') {
      setIsForfeit(false);
      setForfeitedBy(null);
    } else {
      setIsForfeit(true);
      setForfeitedBy('TEAM_A');
    }
  };

  const onToggleForfeitB = () => {
    if (isForfeit && forfeitedBy === 'TEAM_B') {
      setIsForfeit(false);
      setForfeitedBy(null);
    } else {
      setIsForfeit(true);
      setForfeitedBy('TEAM_B');
    }
  };

  // Compute footer class
  const footerClass = pool?.name ? groupBadgeClasses(group) : editing ? 'bg-card text-foreground' : 'bg-background text-foreground';

  const handleExport = async () => {
    // Construct a partial Game object for MatchShareCard
    const game: Partial<Game> = {
      id: gameId,
      tournamentId,
      teamA,
      teamB,
      score,
      winnerSide: localWinnerSide,
      court: localCourt,
      scheduledTime: localScheduledTime,
      finished: localFinished,
      round: stage ? ({ stage: stage as any } as any) : undefined,
    };

    await shareMatchImage(game as Game, contextTournament?.name ?? undefined, contextTournament?.club ?? undefined, undefined, undefined, undefined, contextTournament?.level);
  };

  const favorites = isFavoriteProp !== undefined ? null : useFavorites(!editable);
  const { status } = useSession();
  const router = useRouter();

  const computedIsFavorite = favorites ? favorites.favoriteGames.some(g => g.id == gameId) : false;
  const isFavorite = isFavoriteProp !== undefined ? isFavoriteProp : computedIsFavorite;

  const handleToggleFavorite = () => {
    if (status !== 'authenticated') {
      const currentPath = window.location.pathname + window.location.search;
      localStorage.setItem('authReturnUrl', currentPath);
      router.push('/connexion');
    } else if (favorites) {
      favorites.toggleFavoriteGame(Number.parseInt(gameId), isFavorite);
    } else if (onToggleFavoriteProp) {
      onToggleFavoriteProp(Number.parseInt(gameId), isFavorite);
    }
  };

  return (
    <div
      role="presentation"
      aria-busy={isSaving}
      onClick={(e) => {
        if (editing) {
          e.stopPropagation();
        }
      }}
      className={`relative rounded-lg overflow-hidden w-full sm:max-w-[400px] transition-all duration-200 border border-gray-300
        ${editing
          ? 'shadow-2xl bg-edit-bg/30'
          : 'shadow-sm bg-card'
        }
        ${isInProgress ? 'ring-2 ring-red-500/20' : ''}
        `}
    >
      {isSaving && (
        <div className="absolute inset-0 bg-background/40 backdrop-blur-[1px] z-20 flex items-center justify-center" aria-hidden>
          <CenteredLoader />
        </div>
      )}
      <MatchHeader
        badgeLabel={badgeLabel}
        pool={pool}
        editable={editable}
        isInProgress={isInProgress}
        editing={editing}
        isSaving={isSaving}
        onCancel={onCancel}
        onSave={handleSave}
        onEdit={onEdit}
        showExport={!editable && localFinished}
        onExport={handleExport}
        isFavorite={isFavorite}
        onToggleFavorite={handleToggleFavorite}
      />

      {/* Inline TeamScoreRow component */}
      <div className={`divide-y divide-gray-200 ${editing ? 'opacity-70' : ''}`}>
        <TeamScoreRow
          team={teamA}
          teamIndex={0}
          scores={scores[0]}
          editing={editing}
          setScores={(newScores) => setScores((prev) => [newScores, prev[1]])}
          inputRefs={{ current: inputRefs.current[0] }}
          handleKeyDown={handleKeyDown}
          winnerSide={teamAWinnerSide}
          visibleSets={visibleSets}
          computeTabIndex={(tIdx, sIdx) => sIdx * 2 + (tIdx + 1)}
          showChampion={showChampionA}
          forfeited={isForfeit && forfeitedBy === 'TEAM_A'}
          showAbSlot={isForfeit}
          hideScores={isForfeit && !editing}
          onToggleForfeit={onToggleForfeitA}
        />
        <TeamScoreRow
          team={teamB}
          teamIndex={1}
          scores={scores[1]}
          editing={editing}
          setScores={(newScores) => setScores((prev) => [prev[0], newScores])}
          inputRefs={{ current: inputRefs.current[1] }}
          handleKeyDown={handleKeyDown}
          winnerSide={teamBWinnerSide}
          visibleSets={visibleSets}
          computeTabIndex={(tIdx, sIdx) => sIdx * 2 + (tIdx + 1)}
          showChampion={showChampionB}
          forfeited={isForfeit && forfeitedBy === 'TEAM_B'}
          showAbSlot={isForfeit}
          hideScores={isForfeit && !editing}
          onToggleForfeit={onToggleForfeitB}
        />
      </div>

      {/* Indicateur match en cours - en mode non-éditable */}
      {isInProgress && !editable && (
        <div className="absolute top-2 right-2 z-30">
          <LiveMatchIndicator showLabel={true} />
        </div>
      )}

      <MatchFooter
        editing={editing}
        footerClass={footerClass}
        localCourt={localCourt}
        localScheduledTime={localScheduledTime}
        setLocalCourt={setLocalCourt}
        setLocalScheduledTime={setLocalScheduledTime}
        court={court}
        scheduledTime={scheduledTime}
        handleSave={handleSave}
      />
    </div>
  );
}
