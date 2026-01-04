// File : app/layout/RankingPageLayout.tsx
'use client'

import { Suspense } from 'react';
import GenericPageLayout from './GenericPageLayout';
import PlayerTable from './PlayerTable';
import NationalityFilter from './filters/NationalityFilter';
import ClubFilter from './filters/ClubFilter';
import AgeFilter from './filters/AgeFilter';
import { getUniqueNationalities, getUniqueClubs } from './utils/playerUtils'
import { usePlayerData } from './hooks/usePlayerData';
import { useUrlFilters } from './hooks/useUrlFilters';
import { Player, PlayerFilters } from './PlayerDetailModal';


type Props = {
  jsonUrl: string
}

function RankingPageContent({ jsonUrl }: Props) {
  const initialFiltersState: PlayerFilters = {
    nationalities: [],
    clubs: [],
    ageRange: { min: null, max: null },
    rankingRange: { min: null, max: null },
    pointsRange: { min: null, max: null },
  };

  const { filters, setFilters } = useUrlFilters(initialFiltersState);

  return (
    <GenericPageLayout<Player, PlayerFilters, keyof Player>
      useDataHook={usePlayerData}
      jsonUrl={jsonUrl}
      searchPlaceholder="Rechercher un joueur..."
      initialFilters={filters}
      onFiltersChange={setFilters}
      renderTable={({ data, allData, sortKey, sortOrder, onSort }) => (
        <PlayerTable
          players={data}
          totalPlayers={allData.length}
          sortKey={sortKey}
          sortOrder={sortOrder}
          onSort={onSort}
        />
      )}
      renderFilterContent={({ allData, draftFilters, setDraftFilters }) => (
        <>
          <NationalityFilter
            availableNationalities={getUniqueNationalities(allData)}
            onChange={(updated) => setDraftFilters((prev) => ({ ...prev, nationalities: updated }))}
            selectedNationalities={draftFilters.nationalities || []}
            allPlayers={allData}
          />
          <ClubFilter
            availableClubs={getUniqueClubs(allData)}
            onChange={(updated) => setDraftFilters((prev) => ({ ...prev, clubs: updated }))}
            selectedClubs={draftFilters.clubs || []}
            allPlayers={allData}
          />
          <AgeFilter
            min={draftFilters.ageRange?.min ?? null}
            max={draftFilters.ageRange?.max ?? null}
            onChange={(updated) => setDraftFilters((prev) => ({ ...prev, ageRange: updated }))}
          />
        </>
      )}
    />
  );
}

export default function RankingPageLayout({ jsonUrl }: Props) {
  return (
    <Suspense fallback={<div>Chargement...</div>}>
      <RankingPageContent jsonUrl={jsonUrl} />
    </Suspense>
  );
}