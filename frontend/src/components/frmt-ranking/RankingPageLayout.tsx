// File : app/layout/RankingPageLayout.tsx
'use client'

import GenericPageLayout from './GenericPageLayout';
import PlayerTable from './PlayerTable';
import NationalityFilter from './filters/NationalityFilter';
import ClubFilter from './filters/ClubFilter';
import AgeFilter from './filters/AgeFilter';
import { getUniqueNationalities, getUniqueClubs } from './utils/playerUtils'
import { usePlayerData } from './hooks/usePlayerData';


type Props = {
  jsonUrl: string
}

export default function RankingPageLayout({ jsonUrl }: Props) {
  return (

<GenericPageLayout<Player, PlayerFilters, keyof Player>
      useDataHook={usePlayerData}
      jsonUrl={jsonUrl}
      searchPlaceholder="Rechercher un joueur..."
      renderTable={({ data, sortKey, sortOrder, onSort }) => (
        <PlayerTable
          players={data}
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

  )
}