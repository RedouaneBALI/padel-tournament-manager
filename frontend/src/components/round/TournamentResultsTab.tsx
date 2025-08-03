import { useEffect, useState } from 'react';
import { ArrowDownTrayIcon } from '@heroicons/react/24/solid';
import MatchResultCardLight from '@/src/components/ui/MatchResultCardLight';
import { fetchRounds } from '@/src/utils/fetchRounds';
import { toPng } from 'html-to-image';

// Fonction pour calculer la position verticale correcte de chaque match
function calculateMatchPositions(rounds: Round[]) {
  if (rounds.length === 0) return [];

  const positions: number[][] = [];

  // Calculer les positions pour chaque round
  rounds.forEach((round, roundIndex) => {
    const roundPositions: number[] = [];
    const nbMatches = round.games.length;

    if (roundIndex === 0) {
      // Premier round : positions équidistantes
      const baseSpacing = 160; // Espacement de base entre les matchs
      for (let i = 0; i < nbMatches; i++) {
        roundPositions.push(i * baseSpacing);
      }
    } else {
      // Rounds suivants : chaque match se positionne entre deux matchs précédents
      const previousPositions = positions[roundIndex - 1];
      for (let i = 0; i < nbMatches; i++) {
        // Position au milieu des deux matchs précédents
        const pos1 = previousPositions[i * 2] || 0;
        const pos2 = previousPositions[i * 2 + 1] || 0;
        roundPositions.push((pos1 + pos2) / 2);
      }
    }

    positions.push(roundPositions);
  });

  return positions;
}

// Composant principal
interface TournamentResultsTabProps {
  tournamentId: string;
}

export default function TournamentResultsTab({ tournamentId}: TournamentResultsTabProps) {
  const [rounds, setRounds] = useState<Round[]>([]);



  useEffect(() => {
    async function load() {
      try {
        const data = await fetchRounds(tournamentId);
        setRounds(data);
      } catch (err) {
        console.error("Erreur lors du chargement des rounds : " + err);
      }
    }

    load();
  }, [tournamentId]);

  if (rounds.length === 0) {
    return <p className="text-gray-500">Aucun tirage généré pour le moment.</p>;
  }

  const ROUND_WIDTH = 320;
  const CARD_HEIGHT = 120; // 2 * 60px
  const CARD_MARGIN = 16; // mb-4 = 16px
  const matchPositions = calculateMatchPositions(rounds);

  // Calculer la hauteur totale nécessaire
  const maxPosition = Math.max(...matchPositions.flat()) + 200; // +200 pour la hauteur du dernier match

  const exportBracketAsImage = async () => {
    const node = document.getElementById('bracket-container');
    if (!node) return;

    try {
      const dataUrl = await toPng(node);
      const link = document.createElement('a');
      link.download = 'bracket.png';
      link.href = dataUrl;
      link.click();
    } catch (err) {
      console.error('Erreur lors de l’export en image :', err);
    }
  };

  return (
    <div className="w-full">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xl font-semibold text-gray-900">Arbre du tournoi</h2>
        <button
          onClick={exportBracketAsImage}
          className="p-2 text-white bg-gray-500 hover:bg-blue-700 rounded-md"
          title="Exporter en PNG"
        >
          <ArrowDownTrayIcon className="h-5 w-5" />
        </button>
      </div>

      <div id="bracket-container" className="relative overflow-auto border border-gray-200 rounded-lg p-8 bg-gray-50" style={{ minHeight: `${maxPosition}px` }}>
        <div
          className="relative flex"
          style={{
            width: `${rounds.length * ROUND_WIDTH}px`,
            height: `${maxPosition}px`
          }}
        >
          {rounds.map((round, roundIndex) => (
            <div
              key={round.id}
              className="relative"
              style={{ width: ROUND_WIDTH }}
            >
              {/* Titre du round */}
              <div className="absolute top-0 left-0 right-0 text-center mb-4 text-sm font-semibold text-blue-600">
                {round.stage}
              </div>

              {/* Matchs du round positionnés absolument */}
              {round.games.map((game, gameIndex) => (
                <div
                  key={game.id}
                  className="absolute"
                  style={{
                    top: `${matchPositions[roundIndex][gameIndex] + 40}px`, // +40 pour laisser de la place au titre
                    left: '10px',
                    right: '10px'
                  }}
                >
                  <MatchResultCardLight
                  teamA={game.teamA}
                  teamB={game.teamB}
                  tournamentId={tournamentId}
                  gameId={game.id}
                  score={game.score}
                  editable={false}
                  winnerSide={
                    game.finished
                      ? game.winnerSide === 'TEAM_A'
                        ? 0
                        : game.winnerSide === 'TEAM_B'
                          ? 1
                          : undefined
                      : undefined
                  }
                   />
                 </div>
              ))}

              {/* Lignes de connexion vers le round suivant */}
              {roundIndex < rounds.length - 1 && (
                <div className="absolute top-0 left-0 w-full h-full pointer-events-none">
                  {round.games.map((game, gameIndex) => {
                    // Dessiner les lignes seulement pour les matchs pairs (qui se connectent)
                    if (gameIndex % 2 === 0 && gameIndex + 1 < round.games.length) {
                      // Ajustement pour aligner parfaitement avec la ligne de séparation réelle
                      const currentY1 = matchPositions[roundIndex][gameIndex] + 40 + 61; // +1px d'ajustement pour la bordure
                      const currentY2 = matchPositions[roundIndex][gameIndex + 1] + 40 + 61; // +1px d'ajustement pour la bordure
                      const nextY = matchPositions[roundIndex + 1][Math.floor(gameIndex / 2)] + 40 + 61; // +1px d'ajustement pour la bordure

                      return (
                        <svg
                          key={`connection-${gameIndex}`}
                          className="absolute"
                          style={{
                            left: '290px',
                            top: '0px',
                            width: '40px',
                            height: '100%'
                          }}
                        >
                          {/* Ligne horizontale du premier match */}
                          <line
                            x1="0"
                            y1={currentY1}
                            x2="20"
                            y2={currentY1}
                            stroke="#e5e7eb"
                            strokeWidth="1"
                          />
                          {/* Ligne horizontale du second match */}
                          <line
                            x1="0"
                            y1={currentY2}
                            x2="20"
                            y2={currentY2}
                            stroke="#e5e7eb"
                            strokeWidth="1"
                          />
                          {/* Ligne verticale de connexion */}
                          <line
                            x1="20"
                            y1={currentY1}
                            x2="20"
                            y2={currentY2}
                            stroke="#e5e7eb"
                            strokeWidth="1"
                          />
                          {/* Ligne horizontale vers le match suivant */}
                          <line
                            x1="20"
                            y1={nextY}
                            x2="40"
                            y2={nextY}
                            stroke="#e5e7eb"
                            strokeWidth="1"
                          />
                        </svg>
                      );
                    }
                    return null;
                  })}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}