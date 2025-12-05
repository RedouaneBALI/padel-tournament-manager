import React from 'react';
import { getFlagEmoji } from './utils/flags';
import domtoimage from 'dom-to-image-more';

// Types
interface Player {
  name: string;
  ranking: number;
  points: number;
  nationality: string;
  club: string;
  birth_year: number;
  evolution?: number;
  point_diff?: number;
}

interface Props {
  player: Player;
  onClose: () => void;
}

export default function PlayerDetailModal({ player, onClose }: Props) {
  const modalRef = React.useRef<HTMLDivElement>(null);

  const handleShare = async () => {
    if (!modalRef.current) return;
    // Ajoute la classe pour forcer les couleurs compatibles
    modalRef.current.classList.add('force-html2canvas-colors');
    await new Promise((r) => setTimeout(r, 50)); // Laisse le DOM appliquer la classe
    domtoimage.toPng(modalRef.current, { bgcolor: null, quality: 100 })
      .then((img) => {
        const blob = new Blob([img], { type: 'image/png' });
        const file = new File([blob], `padel-player-${player.name}.png`, { type: 'image/png' });

        if (navigator.share && navigator.canShare({ files: [file] })) {
          // Utilise l'API Web Share si disponible
          navigator.share({
            title: `Classement padel - ${player.name}`,
            text: `Découvrez le classement de ${player.name} sur PadelRounds.com`,
            files: [file],
          }).catch((error) => {
            console.error('Erreur lors du partage:', error);
            // Fallback au téléchargement
            const link = document.createElement('a');
            link.href = img;
            link.download = `padel-player-${player.name}.png`;
            link.click();
          });
        } else {
          // Fallback au téléchargement
          const link = document.createElement('a');
          link.href = img;
          link.download = `padel-player-${player.name}.png`;
          link.click();
        }
      })
      .catch((error) => {
        console.error('Erreur lors de la génération de l\'image:', error);
      });
    modalRef.current.classList.remove('force-html2canvas-colors');
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
      onClick={onClose}
    >
      {/* Modal Card */}
      <div
        ref={modalRef}
        className="relative w-full max-w-sm bg-primary rounded-2xl shadow-lg overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close button */}
        <button
          className="absolute top-4 right-4 z-10 w-8 h-8 flex items-center justify-center rounded-full bg-white/10 text-on-primary hover:bg-white/20 hover:text-on-primary transition-all duration-200"
          onClick={onClose}
          aria-label="Fermer"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
        {/* Share button */}
        <button
          className="absolute top-4 right-14 z-10 w-8 h-8 flex items-center justify-center rounded-full bg-white/10 text-on-primary hover:bg-white/20 hover:text-on-primary transition-all duration-200"
          onClick={handleShare}
          aria-label="Partager"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.367 2.684 3 3 0 00-5.367-2.684z" />
          </svg>
        </button>

        {/* Header */}
        <div className="relative px-6 pt-8 pb-6">
          {/* Ranking badge */}
          <div className="flex items-center justify-center">
            <div className="w-20 h-20 rounded-full bg-accent flex items-center justify-center shadow-md">
              <span className="text-3xl font-bold text-accent-foreground">#{player.ranking}</span>
            </div>
          </div>

          {/* Player name */}
          <h2 className="mt-4 text-2xl font-bold text-on-primary text-center tracking-tight">
            {player.name}
          </h2>

          {/* Nationality */}
          <div className="mt-2 flex items-center justify-center gap-2">
            <span className="text-2xl drop-shadow-lg">{getFlagEmoji(player.nationality)}</span>
            <span className="text-sm font-medium text-on-primary/90">{player.nationality}</span>
          </div>
        </div>

        {/* Main stats - Most important */}
        <div className="relative px-6 pb-4">
          {/* Points - Hero stat */}
          <div className="bg-white/10 backdrop-blur rounded-xl p-4 text-center border border-border">
            <div className="text-4xl font-bold text-on-primary">{player.points.toLocaleString()}</div>
            <div className="text-xs font-semibold text-on-primary/70 uppercase tracking-widest mt-1">Points</div>
          </div>

          {/* Evolution & Point diff */}
          <div className="flex gap-3 mt-3">
            {typeof player.evolution === 'number' && (
              <div className={`flex-1 p-3 rounded-xl text-center ${
                player.evolution > 0 ? 'bg-success/20 border border-success/30' :
                player.evolution < 0 ? 'bg-error/20 border border-error/30' :
                'bg-white/10 border border-border'
              }`}>
                <div className="flex items-center justify-center gap-1">
                  {player.evolution > 0 && (
                    <svg className="w-4 h-4 text-success" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 15.75l7.5-7.5 7.5 7.5" />
                    </svg>
                  )}
                  {player.evolution < 0 && (
                    <svg className="w-4 h-4 text-error" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  )}
                  <span className={`text-xl font-bold ${
                    player.evolution > 0 ? 'text-success' :
                    player.evolution < 0 ? 'text-error' :
                    'text-on-primary/60'
                  }`}>
                    {player.evolution > 0 ? '+' : ''}{player.evolution}
                  </span>
                </div>
                <div className="text-[10px] font-semibold text-on-primary/60 uppercase tracking-wider mt-1">Évolution</div>
              </div>
            )}

            {typeof player.point_diff === 'number' && (
              <div className={`flex-1 p-3 rounded-xl text-center ${
                player.point_diff > 0 ? 'bg-success/20 border border-success/30' :
                player.point_diff < 0 ? 'bg-error/20 border border-error/30' :
                'bg-white/10 border border-border'
              }`}>
                <div className="flex items-center justify-center gap-1">
                  {player.point_diff > 0 && (
                    <svg className="w-4 h-4 text-success" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 15.75l7.5-7.5 7.5 7.5" />
                    </svg>
                  )}
                  {player.point_diff < 0 && (
                    <svg className="w-4 h-4 text-error" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
                    </svg>
                  )}
                  <span className={`text-xl font-bold ${
                    player.point_diff > 0 ? 'text-success' :
                    player.point_diff < 0 ? 'text-error' :
                    'text-on-primary/60'
                  }`}>
                    {player.point_diff > 0 ? '+' : ''}{player.point_diff} pts
                  </span>
                </div>
                <div className="text-[10px] font-semibold text-on-primary/60 uppercase tracking-wider mt-1">Diff. points</div>
              </div>
            )}
          </div>
        </div>

        {/* Secondary info - Less important */}
        <div className="relative px-6 pb-6">
          <div className="flex items-center justify-center gap-4 text-xs text-on-primary/50">
            <span>{player.club}</span>
            <span className="w-1 h-1 rounded-full bg-on-primary/30" />
            <span>{player.birth_year}</span>
          </div>
          <div className="mt-2 text-center text-xl text-text-secondary">padelrounds.com</div>
        </div>
      </div>
    </div>
  );
}
