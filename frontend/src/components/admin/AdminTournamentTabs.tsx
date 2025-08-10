'use client';

import Link from 'next/link';

interface Props {
  tournamentId: string;
  pathname: string;
}

export default function AdminTournamentTabs({ tournamentId, pathname }: Props) {
  const tabClass = (segment: string, exact = false) => {
    const active =
      exact ? pathname.endsWith(segment) : pathname.includes(segment);

    return `pb-2 px-4 font-semibold ${
      active
        ? 'border-b-2 border-primary text-primary'
        : 'text-gray-500 hover:text-primary'
    }`;
  };

  return (
    <div className="flex justify-center mb-6 border-b">
      <Link href={`/admin/tournament/${tournamentId}/players`} className={tabClass('/players', true)}>
        Joueurs
      </Link>
      <Link href={`/admin/tournament/${tournamentId}/rounds/config`} className={tabClass('/rounds/config')}>
        Format
      </Link>
      <Link href={`/admin/tournament/${tournamentId}/games`} className={tabClass('/games')}>
        Matchs
      </Link>
      <Link href={`/admin/tournament/${tournamentId}/rounds/results`} className={tabClass('/rounds/results')}>
        Tableau
      </Link>
    </div>
  );
}