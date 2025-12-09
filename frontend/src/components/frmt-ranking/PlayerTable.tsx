'use client'

import { getFlagEmoji } from './utils/flags'
import { Player } from './PlayerDetailModal'
import DataTable, { ColumnDefinition } from './DataTable'
import { ArrowUp, ArrowDown } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { generatePlayerId } from './utils/playerUtils'
import { useState } from 'react'
import PlayerDetailModal from './PlayerDetailModal'

type Props = {
  players: Player[]
  onSort: (key: keyof Player) => void
  sortKey: keyof Player | null
  sortOrder: 'asc' | 'desc'
}

const playerColumns: ColumnDefinition<Player>[] = [
  {
    key: 'ranking',
    header: '#',
    headerClassName: 'text-center',
    cellClassName: 'text-center font-semibold text-primary',
    mobileWidth: '15vw',
    desktopWidth: '5%',
    renderCell: (player) => (
      <div className="flex flex-col items-center">
        <span>{player.ranking}</span>
        {typeof player.evolution === 'number' && player.evolution !== 0 && (
          <div className="flex items-center opacity-70">
            {Number(player.evolution) > 0 ? (
              <ArrowUp size={8} className="text-green-600" />
            ) : (
              <ArrowDown size={8} className="text-red-600" />
            )}
            <span
              className={`text-[10px] font-normal ml-0.5 ${
                Number(player.evolution) > 0
                  ? 'text-green-600'
                  : 'text-red-600'
              }`}
            >
              {Math.abs(Number(player.evolution))}
            </span>
          </div>
        )}
      </div>
    ),
  },
  {
    key: 'name',
    header: 'Joueur',
    headerClassName: 'text-center',
    cellClassName: 'text-center font-semibold text-primary whitespace-normal break-words',
    mobileWidth: '45vw',
    desktopWidth: '25%',
    renderCell: (player) => player.name,
  },
  {
    key: 'points',
    header: 'Points',
    headerClassName: 'text-center',
    cellClassName: 'text-center font-semibold text-primary',
    mobileWidth: '20vw',
    desktopWidth: '20%',
    renderCell: (player) => (
      <div className="flex flex-col items-center">
        <span>{player.points}</span>
        {typeof player.point_diff === 'number' && player.point_diff !== 0 && (
          <span
            className={`text-[10px] font-normal opacity-70 ${
              player.point_diff > 0 ? 'text-green-600' : 'text-red-600'
            }`}
          >
            {player.point_diff > 0 ? `+${player.point_diff}` : player.point_diff}
          </span>
        )}
      </div>
    ),
  },
  {
    key: 'birth_year',
    header: 'Année',
    headerClassName: 'text-center',
    cellClassName: 'text-center text-primary',
    mobileWidth: '20vw',
    desktopWidth: '20%',
    renderCell: (player) => player.birth_year,
  },
  {
    key: 'nationality',
    header: 'Nat.',
    headerClassName: 'text-center',
    cellClassName: 'text-center',
    desktopWidth: '20%',
    renderCell: (player) => `${getFlagEmoji(player.nationality)} ${player.nationality}`,
  },
  {
    key: 'club',
    header: 'Club',
    headerClassName: 'text-center',
    cellClassName: 'text-center whitespace-nowrap',
    desktopWidth: '20%',
    renderCell: (player) => player.club,
  },
]


export default function PlayerTable({ players, onSort, sortKey, sortOrder }: Props) {
  const [selectedPlayer, setSelectedPlayer] = useState<Player | null>(null);
  const router = useRouter()

  const handleRowClick = (player: Player) => {
    setSelectedPlayer(player);
  }

  return (
    <>
      <DataTable
        data={players}
        columns={playerColumns}
        sortKey={sortKey as string}
        sortOrder={sortOrder}
        onSort={onSort as (key: string) => void}
        getUniqueKey={(player) => `${player.ranking}-${player.name}`}
        onRowClick={handleRowClick}
      />
      {selectedPlayer && (
        <PlayerDetailModal
          player={selectedPlayer}
          onClose={() => setSelectedPlayer(null)}
        />
      )}
    </>
  )
}