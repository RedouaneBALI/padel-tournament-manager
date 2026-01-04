import React, { useRef } from 'react';
import { toBlob } from 'html-to-image';
import { getFlagEmoji } from './utils/flags';
import { Share } from 'lucide-react';

// Types
export interface Player {
  name: string;
  ranking: number;
  points: number;
  nationality: string;
  club: string;
  birth_year: number;
  evolution?: number;
  point_diff?: number;
}

export interface PlayerFilters {
  nationalities: string[];
  clubs: string[];
  rankingRange: { min: number | null; max: number | null };
  pointsRange: { min: number | null; max: number | null };
  ageRange: { min: number | null; max: number | null };
}

interface Props {
  player: Player;
  onClose: () => void;
  totalPlayers: number;
}

/**
 * Calculate the top percentage for a player's ranking
 * Only returns a value if the player is in the top 50%
 */
const getTopPercentage = (ranking: number, totalPlayers: number): number | null => {
  if (totalPlayers === 0) return null;
  const percentage = Math.ceil((ranking / totalPlayers) * 100);
  return percentage <= 50 ? percentage : null;
};

/**
 * Get badge style classes based on the top percentage
 * Returns gradient colors that communicate ranking level intuitively
 */
const getBadgeStyle = (percentage: number): string => {
  if (percentage <= 1) {
    // Top 1% - Gold (prestige maximum)
    return 'bg-gradient-to-br from-yellow-300 to-yellow-500 text-yellow-950 shadow-lg shadow-yellow-400/40 border border-yellow-400/50';
  } else if (percentage <= 5) {
    // Top 2-5% - Silver (excellence)
    return 'bg-gradient-to-br from-slate-300 to-slate-400 text-slate-900 shadow-lg shadow-slate-400/40 border border-slate-400/50';
  } else if (percentage <= 10) {
    // Top 6-10% - Bronze/Orange (très bon)
    return 'bg-gradient-to-br from-orange-400 to-orange-600 text-orange-950 shadow-lg shadow-orange-500/40 border border-orange-500/50';
  } else if (percentage <= 20) {
    // Top 11-20% - Green (bon niveau)
    return 'bg-gradient-to-br from-emerald-400 to-emerald-600 text-emerald-950 shadow-lg shadow-emerald-500/40 border border-emerald-500/50';
  } else if (percentage <= 30) {
    // Top 21-30% - Cyan/Turquoise (correct)
    return 'bg-gradient-to-br from-cyan-400 to-cyan-600 text-cyan-950 shadow-lg shadow-cyan-500/40 border border-cyan-500/50';
  } else if (percentage <= 40) {
    // Top 31-40% - Blue (moyen-supérieur)
    return 'bg-gradient-to-br from-blue-400 to-blue-600 text-blue-950 shadow-lg shadow-blue-500/40 border border-blue-500/50';
  } else if (percentage <= 50) {
    // Top 41-50% - Purple (moyen)
    return 'bg-gradient-to-br from-purple-400 to-purple-600 text-purple-950 shadow-lg shadow-purple-500/40 border border-purple-500/50';
  }
  return 'bg-accent-foreground/10 text-accent-foreground/60';
};

export default function PlayerDetailModal({ player, onClose, totalPlayers }: Props) {
  const cardRef = useRef<HTMLDivElement>(null);

  const handleShare = async () => {
    if (!cardRef.current) return;

    try {
      const blob = await toBlob(cardRef.current, {
        cacheBust: true,
        backgroundColor: 'transparent',
        filter: (node) => {
          if (node instanceof HTMLElement) {
            return !node.classList.contains('ignore-capture');
          }
          return true;
        },
      });

      if (!blob) throw new Error('Échec de la génération de l\'image');

      const fileName = `padel-player-${player.name.replace(/\s+/g, '-').toLowerCase()}.png`;
      const file = new File([blob], fileName, { type: 'image/png' });

      const shareData = {
        title: `Profil de ${player.name}`,
        text: `Découvre les stats de ${player.name} (Rang #${player.ranking}) sur PadelRounds !`,
        files: [file],
      };

      if (navigator.canShare && navigator.canShare(shareData)) {
        try {
          await navigator.share(shareData);
        } catch (err) {
          if ((err as Error).name !== 'AbortError') console.warn('Erreur partage:', err);
        }
      } else {
        console.warn('Partage non pris en charge');
      }

    } catch (error) {
      console.error('Erreur lors de la création de l\'image:', error);
      alert('Impossible de générer l\'image pour le moment.');
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
      onClick={onClose}
      onKeyDown={(e) => {
        if (e.key === 'Escape') {
          onClose();
        }
      }}
      tabIndex={-1}
    >
      {/* Carte Principale */}
      <div
        ref={cardRef}
        className="relative w-full max-w-sm rounded-2xl shadow-lg overflow-hidden border border-white/10"
        style={{
          backgroundImage: 'url("/modale-background.png")',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          backgroundRepeat: 'no-repeat',
        }}
        onClick={(e) => e.stopPropagation()}
        role="presentation"
      >
        {/* Boutons d'action */}
        <div className="absolute top-4 right-4 z-10 flex gap-2 ignore-capture">
          <button
            onClick={handleShare}
            className="w-8 h-8 flex items-center justify-center rounded-full bg-white/10 text-white hover:bg-white/20 transition-colors"
            aria-label="Partager"
          >
            <Share className="w-4 h-4" />
          </button>
          <button
            onClick={onClose}
            className="w-8 h-8 flex items-center justify-center rounded-full bg-white/10 text-white hover:bg-white/20 transition-colors"
            aria-label="Fermer"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* --- Contenu de la carte --- */}

        {/* Header: Nom & Nationalité */}
        <div className="px-6 pt-14 pb-4 text-center">
          <h2 className="text-2xl font-bold text-white tracking-tight">{player.name}</h2>
          <div className="mt-2 flex items-center justify-center gap-2">
            <span className="text-2xl">{getFlagEmoji(player.nationality)}</span>
            <span className="text-sm font-medium text-white/90">{player.nationality}</span>
          </div>
        </div>

        {/* Classement - Élément Principal */}
        <div className="px-6 pb-4">
          <div className="bg-accent rounded-2xl p-6 text-center shadow-lg relative">
            {(() => {
              const topPercentage = getTopPercentage(player.ranking, totalPlayers);
              return topPercentage ? (
                <div className={`absolute top-2 right-2 px-1.5 py-1 rounded-lg ${getBadgeStyle(topPercentage)}`}>
                  <div className="text-xs font-bold uppercase tracking-wider">
                    Top {topPercentage}%
                  </div>
                </div>
              ) : null;
            })()}
            <div className="text-6xl font-black text-accent-foreground tracking-tight">
              #{player.ranking}
            </div>
            <div className="text-sm font-semibold text-accent-foreground/70 uppercase tracking-widest mt-2 flex items-center justify-center gap-2">
              <span>Classement National FRMT</span>
              <span className="text-2xl">🇲🇦</span>
            </div>
            <div className="text-xs text-accent-foreground/50 mt-2">
              <div className="flex items-center justify-center gap-1">
                {(() => {
                  const dateStr = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(new Date());
                  return dateStr.charAt(0).toUpperCase() + dateStr.slice(1);
                })()}
              </div>
            </div>
          </div>
        </div>

        {/* Évolution - 2ème élément important */}
        {typeof player.evolution === 'number' && (
          <div className="px-6 pb-4">
            <div className={`rounded-xl p-4 flex items-center justify-center gap-3 ${
              player.evolution > 0 ? 'bg-green-500/20 border border-green-500/30' :
              player.evolution < 0 ? 'bg-red-500/20 border border-red-500/30' :
              'bg-white/10 border border-white/10'
            }`}>
              {player.evolution !== 0 && (
                player.evolution > 0 ? (
                  <svg className="w-8 h-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 15.75l7.5-7.5 7.5 7.5" />
                  </svg>
                ) : (
                  <svg className="w-8 h-8 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                  </svg>
                )
              )}
              <div className="text-center">
                <div className={`text-3xl font-bold ${
                  player.evolution > 0 ? 'text-green-400' :
                  player.evolution < 0 ? 'text-red-400' :
                  'text-white/60'
                }`}>
                  {player.evolution > 0 && '+'}{player.evolution}
                </div>
                <div className="text-xs font-semibold text-white/50 uppercase tracking-wider">Évolution</div>
              </div>
            </div>
          </div>
        )}

        {/* Stats - Points et Diff */}
        <div className="px-6 pb-4">
          <div className="flex gap-3">
            <div className="flex-1 bg-white/5 backdrop-blur-md rounded-xl p-3 text-center border border-white/5">
              <div className="text-xl font-semibold text-white/80">{player.points.toLocaleString()}</div>
              <div className="text-[10px] font-semibold text-white/40 uppercase tracking-wider mt-1">Points</div>
            </div>
            {typeof player.point_diff === 'number' && (
              <div className={`flex-1 p-3 rounded-xl text-center border flex flex-col items-center justify-center ${
                player.point_diff > 0 ? 'bg-green-500/15 border-green-500/20' :
                player.point_diff < 0 ? 'bg-red-500/15 border-red-500/20' :
                'bg-white/5 border-white/5'
              }`}>
                <div className={`text-xl font-semibold ${
                  player.point_diff > 0 ? 'text-green-400' :
                  player.point_diff < 0 ? 'text-red-400' :
                  'text-white/60'
                }`}>
                  {player.point_diff > 0 && '+'}{player.point_diff}
                </div>
                <div className="text-[10px] font-semibold text-white/40 uppercase tracking-wider mt-1">Diff. Points</div>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 pb-6 text-center">
          <div className="text-xs text-white/40 mb-2">
            {player.club} • {player.birth_year}
          </div>
          <div className="text-lg font-semibold text-white/70 drop-shadow-lg">padelrounds.com</div>
        </div>
      </div>
    </div>
  );
}
