import { useEffect, useState } from 'react';

// Types simulés pour la démo
interface PlayerPair {
  id: string;
  player1?: { name: string };
  player2?: { name: string };
  seed?: number;
}

interface Game {
  id: string;
  teamA: PlayerPair | null;
  teamB: PlayerPair | null;
}

interface Round {
  id: string;
  stage: string;
  games: Game[];
}

// Composant MatchResultCardLight
function MatchResultCardLight({ teamA, teamB }: { teamA: PlayerPair | null; teamB: PlayerPair | null }) {
  const renderPair = (pair: PlayerPair | null) => {
    return (
      <div className="flex items-center px-4 h-[60px]">
        <div className="flex flex-col flex-1">
          <span className="text-sm text-gray-900 truncate">
            {pair?.player1?.name || 'TBD'}
          </span>
          <span className="text-sm text-gray-900 truncate">
            {pair?.player2?.name || ''}
          </span>
        </div>

        <div className="flex items-center space-x-2">
          {pair?.seed && (
            <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded-full font-medium">
              #{pair.seed}
            </span>
          )}
          <div className="w-12 text-center">
            <div className="text-xs text-gray-400 opacity-50"></div>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="bg-white border border-gray-200 rounded-lg shadow-sm overflow-hidden min-w-[280px] max-w-[400px]">
      <div className="divide-y divide-gray-200">
        <div>{renderPair(teamA)}</div>
        <div>{renderPair(teamB)}</div>
      </div>
    </div>
  );
}

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
export default function TournamentResultsTab({ tournamentId }: { tournamentId: string }) {
  const [rounds, setRounds] = useState<Round[]>([]);

  useEffect(() => {
    async function fetchRounds() {
      try {
        const response = await fetch(`http://localhost:8080/tournaments/${tournamentId}/rounds`);
        if (!response.ok) throw new Error();
        const data: Round[] = await response.json();
        setRounds(data);
      } catch (err) {
        console.error("Erreur lors du chargement des rounds : " + err);
      }
    }

    fetchRounds();
  }, [tournamentId]);

  if (rounds.length === 0) {
    return <p className="text-gray-500">Aucun tirage généré pour le moment.</p>;
  }

  const ROUND_WIDTH = 320;
  const matchPositions = calculateMatchPositions(rounds);

  // Calculer la hauteur totale nécessaire
  const maxPosition = Math.max(...matchPositions.flat()) + 200; // +200 pour la hauteur du dernier match

  return (
    <div className="w-full">
      <h2 className="text-xl font-semibold text-gray-900 mb-4">Arbre du tournoi</h2>

      <div className="relative overflow-auto border border-gray-200 rounded-lg p-8 bg-gray-50" style={{ minHeight: `${maxPosition}px` }}>
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
                  <MatchResultCardLight teamA={game.teamA} teamB={game.teamB} />
                </div>
              ))}

              {/* Lignes de connexion vers le round suivant */}
              {roundIndex < rounds.length - 1 && (
                <div className="absolute top-0 left-0 w-full h-full pointer-events-none">
                  {round.games.map((game, gameIndex) => {
                    // Dessiner les lignes seulement pour les matchs pairs (qui se connectent)
                    if (gameIndex % 2 === 0 && gameIndex + 1 < round.games.length) {
                      const currentY1 = matchPositions[roundIndex][gameIndex] + 40 + 60; // Centre du premier match
                      const currentY2 = matchPositions[roundIndex][gameIndex + 1] + 40 + 60; // Centre du second match
                      const nextY = matchPositions[roundIndex + 1][Math.floor(gameIndex / 2)] + 40 + 60; // Centre du match suivant

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