// src/components/tournament/TournamentOverviewTab.tsx
'use client';

import React, { useState } from 'react';
import { Tournament, getGroupsConfig, getKnockoutConfig } from '@/src/types/tournament';
import MatchFormatForm from '@/src/components/round/MatchFormatForm';
import RoundSelector from '@/src/components/round/RoundSelector';
import { MapPin, Building2, Users2, Gauge, Layers, Hash, CalendarDays, CalendarRange, Info } from 'lucide-react';

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
        <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none mb-4">
          Informations générales
        </h3>
        <div className="h-px flex-1 bg-border" />
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* Description (full width) */}
        <div className="sm:col-span-2 rounded-lg border bg-card/50 p-4 relative">
          <div className="absolute -top-2 left-3 bg-card px-1 text-xs uppercase tracking-wide text-muted-foreground">
            Description
          </div>
          <div className="flex items-center gap-3">
            <Info className="h-5 w-5 text-muted-foreground" aria-hidden />
            <div className="min-w-0">
              <div className="text-sm leading-relaxed break-words">
                {tournament.description || '—'}
              </div>
            </div>
          </div>
        </div>

        {/* Localisation (Club + Ville) */}
        <div className="rounded-lg border bg-card/50 p-4 relative">
          <div className="absolute -top-2 left-3 bg-card px-1 text-xs uppercase tracking-wide text-muted-foreground">
            Localisation
          </div>
          <div className="flex items-center gap-3">
            <MapPin className="h-5 w-5 text-muted-foreground" aria-hidden />
            <div className="min-w-0 w-full">
              <div className="text-sm font-medium truncate">
                {(tournament.club || '—') + (tournament.city ? ` (${tournament.city})` : '')}
              </div>
            </div>
          </div>
        </div>

        {/* Catégorie (Genre - Niveau) */}
        <div className="rounded-lg border bg-card/50 p-4 relative">
          <div className="absolute -top-2 left-3 bg-card px-1 text-xs uppercase tracking-wide text-muted-foreground">
            Catégorie
          </div>
          <div className="flex items-center gap-3">
            <Users2 className="h-5 w-5 text-muted-foreground" aria-hidden />
            <div className="min-w-0 w-full">
              <div className="text-sm font-medium">
                {(tournament.gender || '—') + ' - ' + (tournament.level || '—')}
              </div>
            </div>
          </div>
        </div>

        {/* Format + Têtes de série */}
        <div className="rounded-lg border bg-card/50 p-4 relative">
          <div className="absolute -top-2 left-3 bg-card px-1 text-xs uppercase tracking-wide text-muted-foreground">
            FORMAT DU TOURNOI
          </div>
          <div className="flex items-center gap-3">
            <Layers className="h-5 w-5 text-muted-foreground" aria-hidden />
            <div className="min-w-0 w-full">
              <div className="text-sm font-medium">
                {(() => {
                  const seeds = getGroupsConfig(tournament)?.nbSeeds
                    ?? getKnockoutConfig(tournament)?.nbSeeds
                    ?? tournament.nbSeeds;
                  return `${tournament.tournamentFormat ?? '—'}${seeds ? ` - ${seeds} têtes de série` : ''}`;
                })()}
              </div>
            </div>
          </div>
        </div>

        {/* Date (Début — Fin) */}
        <div className="rounded-lg border bg-card/50 p-4 relative">
          <div className="absolute -top-2 left-3 bg-card px-1 text-xs uppercase tracking-wide text-muted-foreground">
            Date
          </div>
          <div className="flex items-center gap-3">
            <CalendarDays className="h-5 w-5 text-muted-foreground" aria-hidden />
            <div className="min-w-0 w-full">
              <div className="text-sm font-medium">
                {tournament.startDate && tournament.endDate
                  ? `${new Date(tournament.startDate).toLocaleDateString('fr-FR')} — ${new Date(tournament.endDate).toLocaleDateString('fr-FR')}`
                  : '—'}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Divider & section header */}
      <div className="mt-8 mb-4 flex items-center gap-3">
        <div className="h-px flex-1 bg-border" />
        <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none">Formats des matchs</h3>
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