'use client';

import React, { useState, useMemo, useEffect } from 'react';
import { usePathname } from 'next/navigation';
import BottomNav from '@/src/components/ui/BottomNav';
import { getDefaultBottomItems } from '@/src/components/ui/bottomNavPresets';
import {
  BAREMES,
  TOURNAMENT_LEVELS,
  TEAM_RANGES,
  TOP_100_CONFIGS,
  requiresTop100Selection,
  getTop100Multiplier,
  applyTop100Multiplier,
  type TournamentLevelKey,
  type TeamRangeKey
} from './baremes';

export default function PointsCalculatorPage() {
  const [tournamentLevel, setTournamentLevel] = useState<string>('P100');
  const [teamRange, setTeamRange] = useState<string>('29+');
  const [ranking, setRanking] = useState<string>('');
  const [top100TeamsCount, setTop100TeamsCount] = useState<string>('');

  const pathname = usePathname() ?? '';
  const bottomItems = getDefaultBottomItems();

  // Vérifier si le niveau sélectionné nécessite la sélection du Top 100
  const showTop100Selection = useMemo(() => {
    return requiresTop100Selection(tournamentLevel);
  }, [tournamentLevel]);

  const top100Config = useMemo(() => {
    return TOP_100_CONFIGS[tournamentLevel];
  }, [tournamentLevel]);

  // Filtrer les options de nombre d'équipes selon le niveau de tournoi sélectionné
  const availableTeamRanges = useMemo(() => {
    if (!tournamentLevel) return TEAM_RANGES;

    return TEAM_RANGES.filter((range) => {
      const bareme = BAREMES[tournamentLevel as TournamentLevelKey]?.[range.value as TeamRangeKey];
      return bareme && bareme.length > 0;
    });
  }, [tournamentLevel]);

  // Réinitialiser le nombre d'équipes si l'option sélectionnée n'est plus disponible
  useEffect(() => {
    if (tournamentLevel && teamRange) {
      const isAvailable = availableTeamRanges.some(range => range.value === teamRange);
      if (!isAvailable) {
        setTeamRange('');
        setRanking('');
      }
    }
    // Réinitialiser top100TeamsCount si le niveau ne nécessite plus cette sélection
    if (!requiresTop100Selection(tournamentLevel)) {
      setTop100TeamsCount('');
    }
  }, [tournamentLevel, teamRange, availableTeamRanges]);

  const calculatedPoints = useMemo(() => {
    if (!tournamentLevel || !teamRange || !ranking) {
      return null;
    }

    // Vérifier si on a besoin de top100TeamsCount et s'il est manquant
    if (showTop100Selection && !top100TeamsCount) {
      return null;
    }

    // Récupérer le barème pour le niveau de tournoi sélectionné
    const bareme = BAREMES[tournamentLevel as TournamentLevelKey]?.[teamRange as TeamRangeKey];
    if (!bareme || bareme.length === 0) return null;

    let basePoints: number | null = null;

    // Vérifier si c'est une plage (ex: "1-4" ou "16-20")
    if (ranking.includes('-')) {
      const parts = ranking.split('-').map(p => p.trim());
      if (parts.length === 2) {
        const start = parseInt(parts[0], 10);
        const end = parseInt(parts[1], 10);

        if (isNaN(start) || isNaN(end) || start < 1 || end < start) {
          return null;
        }

        // Calculer la moyenne des points pour la plage
        let sum = 0;
        let count = 0;

        for (let i = start; i <= end; i++) {
          const index = i - 1;
          if (index < bareme.length) {
            sum += bareme[index];
            count++;
          } else {
            // Si on dépasse le barème, utiliser le dernier points
            sum += bareme[bareme.length - 1];
            count++;
          }
        }

        basePoints = count > 0 ? Math.round(sum / count) : null;
      }
    } else {
      // Sinon, traitement d'un classement simple
      const rankingNumber = parseInt(ranking, 10);
      if (isNaN(rankingNumber) || rankingNumber < 1) {
        return null;
      }

      // Le classement commence à 1, donc index = ranking - 1
      const index = rankingNumber - 1;

      if (index >= bareme.length) {
        // Si le classement dépasse le barème, retourner le dernier points
        basePoints = bareme[bareme.length - 1];
      } else {
        basePoints = bareme[index];
      }
    }

    // Appliquer le multiplicateur Top 100 si nécessaire
    if (basePoints !== null && showTop100Selection) {
      const multiplier = getTop100Multiplier(tournamentLevel, parseInt(top100TeamsCount, 10));
      return applyTop100Multiplier(basePoints, multiplier);
    }

    return basePoints;
  }, [tournamentLevel, teamRange, ranking, top100TeamsCount, showTop100Selection]);

  // Message d'erreur spécifique
  const errorMessage = useMemo(() => {
    if (!tournamentLevel || !teamRange || !ranking) return null;

    const bareme = BAREMES[tournamentLevel as TournamentLevelKey]?.[teamRange as TeamRangeKey];
    if (!bareme || bareme.length === 0) {
      return "Cette combinaison niveau/équipes n'est pas disponible.";
    }

    // Vérifier si c'est une plage
    if (ranking.includes('-')) {
      const parts = ranking.split('-').map(p => p.trim());
      if (parts.length === 2) {
        const start = parseInt(parts[0], 10);
        const end = parseInt(parts[1], 10);

        if (isNaN(start) || isNaN(end)) {
          return "Format de plage invalide. Utilisez le format: 1-4 ou 16-20";
        }

        if (start < 1) {
          return "Le classement doit commencer à partir de 1.";
        }

        if (end < start) {
          return "La fin de la plage doit être supérieure ou égale au début.";
        }
      } else {
        return "Format de plage invalide. Utilisez le format: 1-4 ou 16-20";
      }
      return null;
    }

    // Vérification d'un classement simple
    const rankingNumber = parseInt(ranking, 10);
    if (isNaN(rankingNumber) || rankingNumber < 1) {
      return "Le classement doit être un nombre positif.";
    }


    return null;
  }, [tournamentLevel, teamRange, ranking]);

  return (
    <>
      <div className="container mx-auto py-8 px-4 pb-24">
        <div className="max-w-2xl mx-auto">
          <div className="mb-8">
            <h1 className="text-4xl font-bold mb-2">Calculateur de Points</h1>
            <p className="text-gray-600">
              Calculez les points gagnés selon votre classement dans un tournoi FRMT.
            </p>
          </div>

          <div className="bg-white rounded-lg shadow-md border border-gray-200">
            <div className="p-6 space-y-6">
              {/* Niveau du tournoi */}
              <div className="space-y-2">
                <label htmlFor="tournament-level" className="block text-sm font-medium text-gray-700">
                  Niveau du tournoi
                </label>
                <select
                  id="tournament-level"
                  value={tournamentLevel}
                  onChange={(e) => setTournamentLevel(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">Sélectionnez le niveau</option>
                  {TOURNAMENT_LEVELS.map((level) => (
                    <option key={level.value} value={level.value}>
                      {level.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Nombre d'équipes */}
              <div className="space-y-2">
                <label htmlFor="team-range" className="block text-sm font-medium text-gray-700">
                  Nombre d&apos;équipes participantes
                </label>
                <select
                  id="team-range"
                  value={teamRange}
                  onChange={(e) => setTeamRange(e.target.value)}
                  disabled={!tournamentLevel}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                >
                  <option value="">Sélectionnez le nombre d&apos;équipes</option>
                  {availableTeamRanges.map((range) => (
                    <option key={range.value} value={range.value}>
                      {range.label}
                    </option>
                  ))}
                </select>
                {tournamentLevel && availableTeamRanges.length < TEAM_RANGES.length && (
                  <p className="text-xs text-gray-500">
                    Seuls les créneaux disponibles pour ce niveau sont affichés
                  </p>
                )}
              </div>

              {/* Équipes Top 100 (pour P250 et supérieur) */}
              {showTop100Selection && top100Config && (
                <div className="space-y-2">
                  <label htmlFor="top100-teams" className="block text-sm font-medium text-gray-700">
                    Nombre d&apos;équipes Top 100
                  </label>
                  <select
                    id="top100-teams"
                    value={top100TeamsCount}
                    onChange={(e) => setTop100TeamsCount(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="">Sélectionnez...</option>
                    {top100Config.options.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                  <p className="text-xs text-gray-500">
                    Le nombre d&apos;équipes Top 100 affecte le multiplicateur de points attribués
                  </p>
                </div>
              )}

              {/* Classement */}
              <div className="space-y-2">
                <label htmlFor="ranking" className="block text-sm font-medium text-gray-700">
                  Votre classement final
                </label>
                <input
                  id="ranking"
                  type="text"
                  placeholder="Ex: 1, 3, 1-4, 16-20..."
                  value={ranking}
                  onChange={(e) => setRanking(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
                <p className="text-xs text-gray-500">
                  Vous pouvez saisir un classement simple (ex: 3) ou une plage (ex: 1-4 pour la moyenne des places 1 à 4)
                </p>
              </div>

              {/* Résultat */}
              {calculatedPoints !== null && (
                <div className="mt-8 p-6 bg-blue-50 rounded-lg border-2 border-blue-500">
                  <div className="text-center">
                    <p className="text-sm text-gray-600 mb-2">
                      {ranking.includes('-') ? 'Points moyens gagnés' : 'Points gagnés'}
                    </p>
                    <p className="text-5xl font-bold text-blue-600">
                      {calculatedPoints}
                    </p>
                    <p className="text-sm text-gray-600 mt-2">
                      {ranking.includes('-') ? 'points (moyenne)' : 'points'}
                    </p>
                  </div>
                </div>
              )}

              {tournamentLevel && teamRange && ranking && calculatedPoints === null && errorMessage && (
                <div className="mt-8 p-6 bg-red-50 rounded-lg border border-red-500">
                  <p className="text-center text-red-600">
                    {errorMessage}
                  </p>
                </div>
              )}
            </div>
          </div>

          <div className="mt-6 p-4 bg-gray-100 rounded-lg">
            <p className="text-sm text-gray-600">
              Pour plus de détails, consultez{' '}
              <a
                href="https://www.frmt.ma/guide-de-la-competition-de-padel/"
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-600 hover:text-blue-800 underline font-medium"
              >
                le site officiel de la FRMT
              </a>
              .
            </p>
          </div>
        </div>
      </div>

      <BottomNav items={bottomItems} pathname={pathname} />
    </>
  );
}

