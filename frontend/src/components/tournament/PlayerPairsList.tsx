import { PlayerPair } from '@/src/types/playerPair';

interface PlayerPairListProps {
  pairs: PlayerPair[];
}

export default function PlayerPairList({ pairs }: PlayerPairListProps) {
  if (pairs.length === 0) {
    return <p className="text-gray-500 italic">Aucune paire inscrite pour le moment.</p>;
  }

  return (
    <ul className="space-y-2">
      {pairs.map((pair, index) => (
        <li key={index} className="border rounded px-4 py-2 bg-gray-50 shadow-sm text-sm">
          <span className="font-semibold text-primary">
            {pair.seed && pair.seed > 0 ? `#${pair.seed} ` : ''}
          </span>
          {pair.player1.name} – {pair.player2.name}
        </li>
      ))}
    </ul>
  );
}