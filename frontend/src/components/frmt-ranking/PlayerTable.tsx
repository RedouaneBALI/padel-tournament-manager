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
    headerClassName: 'text-center w-12',
    cellClassName: 'text-center font-semibold text-primary truncate',
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
    headerClassName: 'text-center max-w-[250px] w-[200px]',
    cellClassName: 'text-center font-semibold text-primary whitespace-normal break-words',
    renderCell: (player) => player.name,
  },
  {
    key: 'points',
    header: 'Points',
    headerClassName: 'text-center w-20',
    cellClassName: 'text-center font-semibold text-primary truncate',
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
    headerClassName: 'text-center w-20',
    cellClassName: 'text-center text-primary truncate',
    renderCell: (player) => player.birth_year,
  },
  {
    key: 'nationality',
    header: 'Nationalité',
    headerClassName: 'text-center w-32',
    cellClassName: 'text-center truncate',
    renderCell: (player) => `${getFlagEmoji(player.nationality)} ${player.nationality}`,
  },
  {
    key: 'club',
    header: 'Club',
    headerClassName: 'text-center w-25',
    cellClassName: 'text-center whitespace-nowrap',
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