import React, { useState, useEffect } from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import KnockoutPlayerAssignment from '@/src/components/admin/KnockoutPlayerAssignment';
import GroupsKoPlayerAssignment from '@/src/components/admin/GroupsKoPlayerAssignment';
import QualifKoPlayerAssignment from '@/src/components/admin/QualifKoPlayerAssignment';
import { fetchPairs } from '@/src/api/tournamentApi';

interface Props {
  tournament: Tournament;
}

export default function AdminTournamentPlayerAssignment({ tournament }: Props) {
  const [playerPairs, setPlayerPairs] = useState<PlayerPair[]>([]);
  const format = tournament?.format;

  useEffect(() => {
    async function loadPairs() {
      const pairs = await fetchPairs(tournament.id, true);
      setPlayerPairs(pairs);
    }
    loadPairs();
  }, [tournament.id]);

  if (format === 'KNOCKOUT') {
    return <KnockoutPlayerAssignment tournament={tournament} playerPairs={playerPairs} />;
  }

  if (format === 'GROUPS_KO') {
    return <GroupsKoPlayerAssignment tournament={tournament} playerPairs={playerPairs} />;
  }

  if (format === 'QUALIF_KO') {
    return <QualifKoPlayerAssignment tournament={tournament} playerPairs={playerPairs} />;
  }

  return <p className="text-tab-inactive italic">Format non géré</p>;
}