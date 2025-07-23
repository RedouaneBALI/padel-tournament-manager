'use client';

import { useEffect, useState } from 'react';
import { Tournament } from '@/app/types/Tournament';
import PlayerPairsTextarea from './PlayerPairsTextarea';
import { SimplePlayerPair } from '@/app/types/PlayerPair';

interface Props {
  tournament: Tournament;
}

export default function TournamentPlayersTabWrapper({ tournament }: Props) {
  const [playerPairs, setPlayerPairs] = useState<SimplePlayerPair[]>([]);


  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">In progress...</h2>
    </div>
  );
}