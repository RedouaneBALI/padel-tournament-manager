import React, { useRef } from 'react';
import { toBlob } from 'html-to-image';
import { getFlagEmoji } from './utils/flags';

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
  const cardRef = useRef<HTMLDivElement>(null);

  const handleShare = async () => {
    if (!cardRef.current) return;

    try {
      // 1. Génération du Blob avec html-to-image
      const blob = await toBlob(cardRef.current, {
        cacheBust: true, // Évite les problèmes de cache d'image
        backgroundColor: '#1b2d5e', // Force le fond bleu (bg-primary)
        // Filtre pour exclure les éléments ayant la classe 'ignore-capture'
        filter: (node) => {
          // Vérification de sécurité car certains noeuds (textes) n'ont pas de classList
          if (node instanceof HTMLElement) {
            return !node.classList.contains('ignore-capture');
          }
          return true;
        },
      });

      if (!blob) throw new Error('Échec de la génération de l\'image');

      // 2. Préparation du fichier
      const fileName = `padel-player-${player.name.replace(/\s+/g, '-').toLowerCase()}.png`;
      const file = new File([blob], fileName, { type: 'image/png' });

      const shareData = {
        title: `Profil de ${player.name}`,
        text: `Découvre les stats de ${player.name} (Rang #${player.ranking}) sur PadelRounds !`,
        files: [file],
      };

      // 3. Logique de partage Mobile (Prioritaire)
      // On vérifie si le navigateur supporte le partage de fichiers
      if (navigator.canShare && navigator.canShare(shareData)) {
        try {
          await navigator.share(shareData);
          return; // Si le partage fonctionne, on s'arrête là
        } catch (err) {
          // Ignore l'erreur si l'utilisateur annule le partage
          if ((err as Error).name !== 'AbortError') console.warn('Erreur partage:', err);
        }
      }

      // 4. Fallback : Téléchargement direct (Desktop ou échec share)
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      // Nettoyage mémoire
      setTimeout(() => URL.revokeObjectURL(url), 100);

    } catch (error) {
      console.error('Erreur lors de la création de l\'image:', error);
      alert('Impossible de générer l\'image pour le moment.');
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
      onClick={onClose}
    >
      {/* Carte Principale */}
      <div
        ref={cardRef}
        className="relative w-full max-w-sm bg-primary rounded-2xl shadow-lg overflow-hidden border border-white/10"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Boutons d'action (Exclus de la capture via la classe 'ignore-capture') */}
        <div className="absolute top-4 right-4 z-10 flex gap-2 ignore-capture">
          <button
            onClick={handleShare}
            className="w-8 h-8 flex items-center justify-center rounded-full bg-white/10 text-white hover:bg-white/20 transition-colors"
            aria-label="Partager"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.367 2.684 3 3 0 00-5.367-2.684z" />
            </svg>
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

        {/* Header: Photo & Ranking */}
        <div className="px-6 pt-8 pb-6 text-center">
          <div className="mx-auto w-20 h-20 rounded-full bg-accent flex items-center justify-center shadow-lg mb-4">
            <span className="text-3xl font-bold text-accent-foreground">#{player.ranking}</span>
          </div>

          <h2 className="text-2xl font-bold text-white tracking-tight">{player.name}</h2>

          <div className="mt-2 flex items-center justify-center gap-2">
            <span className="text-2xl">{getFlagEmoji(player.nationality)}</span>
            <span className="text-sm font-medium text-white/90">{player.nationality}</span>
          </div>
        </div>

        {/* Stats Grid */}
        <div className="px-6 pb-4 space-y-3">
          {/* Main Points */}
          <div className="bg-white/10 backdrop-blur-md rounded-xl p-4 text-center border border-white/10 shadow-inner">
            <div className="text-4xl font-bold text-white">{player.points.toLocaleString()}</div>
            <div className="text-xs font-semibold text-white/60 uppercase tracking-widest mt-1">Points</div>
          </div>

          {/* Evolution Rows */}
          <div className="flex gap-3">
            {renderStatBox(player.evolution, 'Évolution')}
            {renderStatBox(player.point_diff, 'Diff. points', true)}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 pb-6 text-center">
          <div className="text-xs text-white/40 mb-2">
            {player.club} • {player.birth_year}
          </div>
          <div className="text-sm font-medium text-white/20">padelrounds.com</div>
        </div>
      </div>
    </div>
  );
}

// Helper pour le rendu conditionnel propre des boites de stats
const renderStatBox = (value: number | undefined, label: string, isPoints = false) => {
  if (typeof value !== 'number') return null;

  const isPositive = value > 0;
  const isNegative = value < 0;

  // Utilisation des classes Tailwind standard (compatible v4 maintenant grâce à html-to-image)
  let bgClass = 'bg-white/10';
  let borderClass = 'border-white/10';
  let textClass = 'text-white/60';
  let icon = null;

  if (isPositive) {
    bgClass = 'bg-green-500/20';
    borderClass = 'border-green-500/30';
    textClass = 'text-green-400';
    icon = (
      <svg className="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 15.75l7.5-7.5 7.5 7.5" />
      </svg>
    );
  } else if (isNegative) {
    bgClass = 'bg-red-500/20';
    borderClass = 'border-red-500/30';
    textClass = 'text-red-400';
    icon = (
      <svg className="w-3 h-3 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
      </svg>
    );
  }

  return (
    <div className={`flex-1 p-3 rounded-xl text-center border ${bgClass} ${borderClass} flex flex-col items-center justify-center`}>
      <div className={`text-xl font-bold flex items-center ${textClass}`}>
        {icon}
        {isPositive && '+'}{value}{isPoints && ' pts'}
      </div>
      <div className="text-[10px] font-semibold text-white/50 uppercase tracking-wider mt-1">{label}</div>
    </div>
  );
};