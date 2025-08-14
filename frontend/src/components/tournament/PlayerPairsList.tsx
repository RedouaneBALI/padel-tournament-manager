import { PlayerPair } from '@/src/types/playerPair';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

interface PlayerPairListProps {
  pairs: PlayerPair[];
  loading?: boolean;
}

export default function PlayerPairList({ pairs, loading = false }: PlayerPairListProps) {
    const hasPairs = (pairs?.length ?? 0) > 0;

    if (!hasPairs) {
      if (loading) {
        return <CenteredLoader />;
      }
      return <p className="text-muted italic">Aucune paire inscrite pour le moment.</p>;
    }

  return (
    <ul className="space-y-2">
      {pairs.map((pair, index) => (
        <li key={index} className="border rounded px-4 py-2 bg-background shadow-sm text-sm">
          <span className="font-semibold text-primary">
            {pair.seed && pair.seed > 0 ? `#${pair.seed} ` : ''}
          </span>
          {pair.player1.name} – {pair.player2.name}
        </li>
      ))}
    </ul>
  );
}