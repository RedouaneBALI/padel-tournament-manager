import React from 'react';
import { Tournament } from '@/src/types/tournament';
import { PlayerPair } from '@/src/types/playerPair';
import KnockoutPlayerAssignment from '@/src/components/admin/KnockoutPlayerAssignment';
import GroupsKoPlayerAssignment from '@/src/components/admin/GroupsKoPlayerAssignment';
import QualifKoPlayerAssignment from '@/src/components/admin/QualifKoPlayerAssignment';

interface Props {
  tournament: Tournament;
  playerPairs: PlayerPair[];
}

export default function AdminTournamentPlayerAssignment({ tournament, playerPairs }: Props) {
  const format = tournament?.format;

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