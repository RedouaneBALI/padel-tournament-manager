import React, { useRef } from 'react';
import { toBlob } from 'html-to-image';
import { Player } from './PlayerDetailModal';
import { getFlagEmoji } from './utils/flags';
import { ArrowUp, ArrowDown } from 'lucide-react';

interface TableExporterProps {
  players: Player[];
  onExportComplete?: () => void;
}

/**
 * Renders an exportable table with up to 10 rows
 * Uses html-to-image to generate a shareable image
 */
export default function TableExporter({ players, onExportComplete }: TableExporterProps) {
  const tableRef = useRef<HTMLDivElement>(null);

  const handleExport = async () => {
    if (!tableRef.current) {
      console.error('[TableExporter] tableRef.current is null!');
      return;
    }

    try {
      const blob = await toBlob(tableRef.current, {
        cacheBust: true,
        backgroundColor: 'transparent',
        pixelRatio: 2,
        width: 1080,
        height: 1920,
      });

      if (!blob) throw new Error('Échec de la génération de l\'image');

      const fileName = `classement-frmt-${new Date().toISOString().split('T')[0]}.png`;
      const file = new File([blob], fileName, { type: 'image/png' });

      const shareData = {
        title: 'Classement FRMT - Top joueurs',
        text: 'Découvre le classement des meilleurs joueurs de padel sur PadelRounds !',
        files: [file],
      };

      if (navigator.canShare && navigator.canShare(shareData)) {
        try {
          await navigator.share(shareData);
        } catch (err) {
          if ((err as Error).name !== 'AbortError') {
            console.warn('[TableExporter] Erreur partage:', err);
          }
        }
      } else {
        // Fallback: download the image
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        link.click();
        URL.revokeObjectURL(url);
      }

      onExportComplete?.();
    } catch (error) {
      console.error('[TableExporter] Erreur lors de la création de l\'image:', error);
      alert('Impossible de générer l\'image pour le moment.');
      onExportComplete?.();
    }
  };

  React.useEffect(() => {
    // Wait for DOM to render before capturing
    const timer = setTimeout(() => {
      handleExport();
    }, 100);

    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const displayedPlayers = players.slice(0, 10);

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center pointer-events-none" style={{ opacity: 0 }}>
      <div
        ref={tableRef}
        className="p-8 flex flex-col"
        style={{
          width: '1080px',
          minWidth: '1080px',
          maxWidth: '1080px',
          height: '1920px',
          minHeight: '1920px',
          maxHeight: '1920px',
          display: 'flex',
          backgroundImage: 'url("/ranking-export-background.png")',
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          backgroundRepeat: 'no-repeat',
        }}
      >
        {/* Header */}
        <div className="mb-8 text-center flex-none">
          <h1 className="text-6xl font-bold text-white mb-3">Classement National FRMT 🇲🇦</h1>
          <p className="text-3xl text-gray-200 font-medium">
            {(() => {
              const dateStr = new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(new Date());
              return dateStr.charAt(0).toUpperCase() + dateStr.slice(1);
            })()}
          </p>
        </div>

        {/* Table - takes remaining space */}
        <div className="flex-1 flex flex-col">
          <table className="w-full border-collapse h-full">
            <thead>
              <tr>
                <th className="p-5 text-center border border-white/20 text-3xl font-bold text-white" style={{ width: '10%', backgroundColor: 'rgba(255, 255, 255, 0.15)' }}>#</th>
                <th className="p-5 text-center border border-white/20 text-3xl font-bold text-white" style={{ width: '50%', backgroundColor: 'rgba(255, 255, 255, 0.15)' }}>Joueur</th>
                <th className="p-5 text-center border border-white/20 text-3xl font-bold text-white" style={{ width: '25%', backgroundColor: 'rgba(255, 255, 255, 0.15)' }}>Points</th>
                <th className="p-5 text-center border border-white/20 text-3xl font-bold text-white" style={{ width: '15%', backgroundColor: 'rgba(255, 255, 255, 0.15)' }}>Année</th>
              </tr>
            </thead>
            <tbody>
              {displayedPlayers.map((player, index) => (
                <tr
                  key={`${player.ranking}-${player.name}`}
                  style={{ height: `${100 / displayedPlayers.length}%` }}
                >
                  <td className="p-6 text-center border border-white/10" style={{ backgroundColor: index % 2 === 0 ? 'rgba(0, 0, 0, 0.3)' : 'rgba(0, 0, 0, 0.2)' }}>
                    <div className="flex flex-col items-center justify-center h-full">
                      <span className="font-bold text-white text-4xl">{player.ranking}</span>
                      {typeof player.evolution === 'number' && player.evolution !== 0 && (
                        <div className="flex items-center text-xl mt-1">
                          {player.evolution > 0 ? (
                            <ArrowUp size={20} className="text-green-400" />
                          ) : (
                            <ArrowDown size={20} className="text-red-400" />
                          )}
                          <span className={player.evolution > 0 ? 'text-green-400 font-semibold' : 'text-red-400 font-semibold'}>
                            {Math.abs(player.evolution)}
                          </span>
                        </div>
                      )}
                    </div>
                  </td>
                  <td className="p-6 text-center border border-white/10 font-bold text-white text-3xl" style={{ backgroundColor: index % 2 === 0 ? 'rgba(0, 0, 0, 0.3)' : 'rgba(0, 0, 0, 0.2)' }}>
                    {player.name} <span className="text-4xl">{getFlagEmoji(player.nationality)}</span>
                  </td>
                  <td className="p-6 text-center border border-white/10" style={{ backgroundColor: index % 2 === 0 ? 'rgba(0, 0, 0, 0.3)' : 'rgba(0, 0, 0, 0.2)' }}>
                    <div className="flex flex-col items-center justify-center h-full">
                      <span className="font-bold text-white text-4xl">{player.points}</span>
                      {typeof player.point_diff === 'number' && player.point_diff !== 0 && (
                        <span
                          className={`text-xl mt-1 font-semibold ${
                            player.point_diff > 0 ? 'text-green-400' : 'text-red-400'
                          }`}
                        >
                          {player.point_diff > 0 ? `+${player.point_diff}` : player.point_diff}
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="p-6 text-center border border-white/10 text-gray-200 text-3xl font-medium" style={{ backgroundColor: index % 2 === 0 ? 'rgba(0, 0, 0, 0.3)' : 'rgba(0, 0, 0, 0.2)' }}>
                    {player.birth_year}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Footer */}
        <div className="mt-8 text-center flex-none">
          <p className="text-4xl font-bold text-white">padelrounds.com</p>
        </div>
      </div>
    </div>
  );
}

