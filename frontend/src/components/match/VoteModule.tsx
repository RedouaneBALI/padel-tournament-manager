import React, { useEffect, useState } from 'react';
import { submitVote, fetchVoteSummary } from '@/src/api/tournamentApi';
import type { VoteSummary } from '@/src/types/vote';
import { ThumbsUp, Trophy, Loader2, RefreshCw } from 'lucide-react';

type VoteModuleProps = {
  gameId: string;
  teamAName?: string;
  teamBName?: string;
  isVotingDisabled?: boolean;
};

export default function VoteModule({
  gameId,
  teamAName = "Équipe A",
  teamBName = "Équipe B",
  isVotingDisabled = false
}: VoteModuleProps) {
  const [voteSummary, setVoteSummary] = useState<VoteSummary | null>(null);
  const [voteLoading, setVoteLoading] = useState<'TEAM_A' | 'TEAM_B' | null>(null);
  const [hasAnimated, setHasAnimated] = useState(false);

  useEffect(() => {
    async function loadVotes() {
      try {
        const summary = await fetchVoteSummary(gameId);
        setVoteSummary(summary);
        setTimeout(() => setHasAnimated(true), 100);
      } catch (e) {
        console.error('Erreur fetchVoteSummary:', e);
      }
    }
    loadVotes();
  }, [gameId]);

  const handleVote = async (teamSide: 'TEAM_A' | 'TEAM_B') => {
    if (voteLoading || voteSummary?.currentUserVote === teamSide) return;

    setVoteLoading(teamSide);
    try {
      const summary = await submitVote(gameId, teamSide);
      setVoteSummary(summary);
    } catch (e) {
      console.error(e);
    } finally {
      setVoteLoading(null);
    }
  };

  if (!voteSummary) {
    return (
      <div className="mt-6 w-full max-w-md mx-auto h-24 bg-muted/50 animate-pulse rounded-xl" />
    );
  }

  const totalVotes = voteSummary.teamAVotes + voteSummary.teamBVotes;

  // Calcul des pourcentages réels
  const rawTeamAPerc = totalVotes > 0 ? (voteSummary.teamAVotes / totalVotes) * 100 : 50;
  const rawTeamBPerc = 100 - rawTeamAPerc;

  // Pour l'animation : on part de 50/50
  const displayTeamAPerc = hasAnimated ? rawTeamAPerc : 50;
  const displayTeamBPerc = 100 - displayTeamAPerc;

  const isVotedA = voteSummary.currentUserVote === 'TEAM_A';
  const isVotedB = voteSummary.currentUserVote === 'TEAM_B';
  const hasVoted = !!voteSummary.currentUserVote;

  return (
    <div className="mt-6 w-full max-w-md mx-auto">
      <div className="relative bg-card rounded-xl border shadow-sm overflow-hidden">

        {/* Header */}
        <div className="px-4 py-3 border-b flex items-center justify-between bg-muted/20">
          <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
            <Trophy className="w-4 h-4 text-yellow-500" />
            <span>Pronostiques</span>
          </div>
          <span className="text-xs font-medium text-muted-foreground bg-background px-2 py-1 rounded-full border">
            {totalVotes} vote{totalVotes !== 1 ? 's' : ''}
          </span>
        </div>

        <div className="p-4">
          {/* Barre de duel (Tug of war) */}
          <div className="relative h-8 w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden flex mb-4 ring-1 ring-inset ring-black/5">

            {/* Côté A (Bleu) */}
            <div
              className="bg-blue-500 h-full flex items-center transition-all duration-700 ease-out relative z-10 overflow-hidden"
              // On ajoute conditionnellement le padding pour éviter qu'il prenne de la place si width=0
              style={{
                width: `${displayTeamAPerc}%`,
                paddingLeft: displayTeamAPerc > 0 ? '0.75rem' : 0
              }}
            >
              {Math.round(rawTeamAPerc) > 10 && (
                <span className="text-xs font-bold text-white whitespace-nowrap drop-shadow-md animate-in fade-in zoom-in duration-300">
                  {Math.round(rawTeamAPerc)}%
                </span>
              )}
            </div>

            {/* Côté B (Rouge) */}
            <div
              className="bg-rose-500 h-full flex items-center justify-end transition-all duration-700 ease-out relative z-0 overflow-hidden"
              style={{
                width: `${displayTeamBPerc}%`,
                paddingRight: displayTeamBPerc > 0 ? '0.75rem' : 0
              }}
            >
               {Math.round(rawTeamBPerc) > 10 && (
                 <span className="text-xs font-bold text-white whitespace-nowrap drop-shadow-md animate-in fade-in zoom-in duration-300">
                  {Math.round(rawTeamBPerc)}%
                </span>
               )}
            </div>

            {/* Séparateur brillant (VS) - masqué si 100% d'un côté pour éviter un trait bizarre sur le bord */}
            {rawTeamAPerc > 0 && rawTeamAPerc < 100 && (
                <div className="absolute inset-y-0 left-1/2 -translate-x-1/2 z-20 w-1 bg-white/20 backdrop-blur-[1px]" />
            )}
          </div>

          {/* Boutons d'actions */}
          {!isVotingDisabled && (
            <div className="grid grid-cols-2 gap-3">
              <button
                onClick={() => handleVote('TEAM_A')}
                disabled={voteLoading !== null || isVotedA}
                className={`
                  group relative flex flex-col items-center justify-center py-3 px-2 rounded-lg border transition-all duration-200
                  ${isVotedA
                    ? 'bg-blue-50/50 border-blue-500 ring-1 ring-blue-500 dark:bg-blue-900/20 cursor-default'
                    : 'hover:bg-muted border-border hover:border-blue-300 hover:shadow-sm active:scale-[0.98]'}
                  ${isVotedB ? 'opacity-70 hover:opacity-100' : ''}
                `}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className={`text-sm font-bold ${isVotedA ? 'text-blue-600' : 'text-foreground group-hover:text-blue-600 transition-colors'}`}>
                    {teamAName}
                  </span>
                  {isVotedA && <div className="w-2 h-2 rounded-full bg-blue-500 animate-pulse" />}
                </div>

                <div className="text-xs text-muted-foreground flex items-center gap-1">
                </div>
              </button>

              <button
                onClick={() => handleVote('TEAM_B')}
                disabled={voteLoading !== null || isVotedB}
                className={`
                  group relative flex flex-col items-center justify-center py-3 px-2 rounded-lg border transition-all duration-200
                  ${isVotedB
                    ? 'bg-rose-50/50 border-rose-500 ring-1 ring-rose-500 dark:bg-rose-900/20 cursor-default'
                    : 'hover:bg-muted border-border hover:border-rose-300 hover:shadow-sm active:scale-[0.98]'}
                  ${isVotedA ? 'opacity-70 hover:opacity-100' : ''}
                `}
              >
                <div className="flex items-center gap-2 mb-1">
                  {isVotedB && <div className="w-2 h-2 rounded-full bg-rose-500 animate-pulse" />}
                  <span className={`text-sm font-bold ${isVotedB ? 'text-rose-600' : 'text-foreground group-hover:text-rose-600 transition-colors'}`}>
                    {teamBName}
                  </span>
                </div>

                <div className="text-xs text-muted-foreground flex items-center gap-1">

                </div>
              </button>
            </div>
          )}

          {isVotingDisabled && (
            <div className="text-center py-2">
              <p className="text-xs text-muted-foreground">Les votes sont fermés - match en cours</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}