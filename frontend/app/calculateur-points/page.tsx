'use client';

import React, { useState, useMemo, useEffect } from 'react';
import { usePathname } from 'next/navigation';
import BottomNav from '@/src/components/ui/BottomNav';
import { getDefaultBottomItems } from '@/src/components/ui/bottomNavPresets';
import { BAREMES, TOURNAMENT_LEVELS, TEAM_RANGES, type TournamentLevelKey, type TeamRangeKey } from './baremes';

export default function PointsCalculatorPage() {
  const [tournamentLevel, setTournamentLevel] = useState<string>('');
  const [teamRange, setTeamRange] = useState<string>('');
  const [ranking, setRanking] = useState<string>('');

  const pathname = usePathname() ?? '';
  const bottomItems = getDefaultBottomItems();
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
  }, [tournamentLevel, teamRange, availableTeamRanges]);

  const calculatedPoints = useMemo(() => {
    if (!tournamentLevel || !teamRange || !ranking) {
      return null;
    }

    const rankingNumber = parseInt(ranking, 10);
    if (isNaN(rankingNumber) || rankingNumber < 1) {
      return null;
    }

    // Récupérer le barème pour le niveau de tournoi sélectionné
    const bareme = BAREMES[tournamentLevel as TournamentLevelKey]?.[teamRange as TeamRangeKey];
    if (!bareme || bareme.length === 0) return null;

    // Le classement commence à 1, donc index = ranking - 1
    const index = rankingNumber - 1;

    if (index >= bareme.length) {
      // Si le classement dépasse le barème, retourner le dernier points
      return bareme[bareme.length - 1];
    }

    return bareme[index];
  }, [tournamentLevel, teamRange, ranking]);

  // Message d'erreur spécifique
  const errorMessage = useMemo(() => {
    if (!tournamentLevel || !teamRange || !ranking) return null;

    const rankingNumber = parseInt(ranking, 10);
    if (isNaN(rankingNumber) || rankingNumber < 1) {
      return "Le classement doit être un nombre positif.";
    }

    const bareme = BAREMES[tournamentLevel as TournamentLevelKey]?.[teamRange as TeamRangeKey];
    if (!bareme || bareme.length === 0) {
      return "Cette combinaison niveau/équipes n'est pas disponible.";
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

              {/* Classement */}
              <div className="space-y-2">
                <label htmlFor="ranking" className="block text-sm font-medium text-gray-700">
                  Votre classement final
                </label>
                <input
                  id="ranking"
                  type="number"
                  min="1"
                  placeholder="Ex: 1, 2, 3..."
                  value={ranking}
                  onChange={(e) => setRanking(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              {/* Résultat */}
              {calculatedPoints !== null && (
                <div className="mt-8 p-6 bg-blue-50 rounded-lg border-2 border-blue-500">
                  <div className="text-center">
                    <p className="text-sm text-gray-600 mb-2">Points gagnés</p>
                    <p className="text-5xl font-bold text-blue-600">
                      {calculatedPoints}
                    </p>
                    <p className="text-sm text-gray-600 mt-2">points</p>
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

