// src/components/tournament/TournamentOverviewTab.tsx
'use client';

import React, { useState } from 'react';
import { Tournament } from '@/src/types/tournament';
import MatchFormatForm from '@/src/components/round/MatchFormatForm';
import RoundSelector from '@/src/components/round/RoundSelector';

interface TournamentOverviewTabProps {
  tournament: Tournament;
}

export default function TournamentOverviewTab({ tournament }: TournamentOverviewTabProps) {
  const rounds = tournament.rounds ?? [];
  const [currentStageIndex, setCurrentStageIndex] = useState<number>(0);
  const currentRound = rounds[currentStageIndex];

  return (
    <section className="bg-card p-6 rounded-md shadow-md">
      {/* Divider & section header */}
      <div className="mb-4 flex items-center gap-3">
        <div className="h-px flex-1 bg-border" />
        <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none">
          Informations générales
        </h3>
        <div className="h-px flex-1 bg-border" />
      </div>
      <p><strong>Nom :</strong> {tournament.name}</p>
      <p><strong>Description :</strong> {tournament.description || '—'}</p>
      <p><strong>Ville :</strong> {tournament.city || '—'}</p>
      <p><strong>Club :</strong> {tournament.club || '—'}</p>
      <p><strong>Genre :</strong> {tournament.gender || '—'}</p>
      <p><strong>Niveau :</strong> {tournament.level || '—'}</p>
      <p><strong>Format :</strong> {tournament.tournamentFormat || '—'}</p>
      <p><strong>Nombre de têtes de série :</strong> {tournament.nbSeeds}</p>
      <p><strong>Date de début :</strong> {tournament.startDate || '—'}</p>
      <p><strong>Date de fin :</strong> {tournament.endDate || '—'}</p>

      {/* Divider & section header */}
      <div className="mt-8 mb-4 flex items-center gap-3">
        <div className="h-px flex-1 bg-border" />
        <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none">Formats des rounds</h3>
        <div className="h-px flex-1 bg-border" />
      </div>

      {/* Section formats (lecture seule, avec sélecteur de round) */}
      <div className="mt-6 space-y-4">
        {rounds.length > 0 ? (
          <>
            <RoundSelector
              rounds={rounds}
              currentIndex={currentStageIndex}
              onChange={setCurrentStageIndex}
            />

            {currentRound?.matchFormat ? (
                <MatchFormatForm format={currentRound.matchFormat} readOnly />
            ) : (
              <p className="text-sm text-muted-foreground">
                Aucun format défini pour ce round.
              </p>
            )}
          </>
        ) : (
          <p className="text-sm text-muted-foreground">Aucun round disponible.</p>
        )}
      </div>
    </section>
  );
}