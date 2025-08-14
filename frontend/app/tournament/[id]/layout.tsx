// app/tournament/[id]/layout.tsx
'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ToastContainer, toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { Tournament } from '@/src/types/tournament';
import React from 'react';
import { ReactNode } from 'react';
import { fetchTournament } from '@/src/api/tournamentApi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

export default function TournamentLayout({children,params,}: {children: ReactNode;params: Promise<{ id: string }>;}){
  const { id } = React.use(params);
  const pathname = usePathname();
  const [tournament, setTournament] = useState<Tournament | null>(null);

  useEffect(() => {
    fetchTournament(id)
      .then(setTournament);
  }, [id]);

  if (!tournament) {
    return (
      <CenteredLoader />
    );
  }

  return (
    <div className="w-full max-w-screen-2xl px-2 sm:px-4 mx-auto">
      <h1 className="text-2xl font-bold mb-4">{tournament.name}</h1>
      <div className="flex justify-center mb-6 border-b border-border">
        <Link
          href={`/tournament/${id}`}
          className={`pb-2 px-4 font-semibold ${pathname === `/tournament/${id}` ? 'border-b-2 border-primary text-primary' : 'text-muted hover:text-primary'}`}
        >
          Aperçu
        </Link>
        <Link
          href={`/tournament/${id}/players`}
          className={`pb-2 px-4 font-semibold ${pathname === `/tournament/${id}/players` ? 'border-b-2 border-primary text-primary' : 'text-muted hover:text-primary'}`}
        >
          Joueurs
        </Link>
        <Link
          href={`/tournament/${id}/games`}
          className={`pb-2 px-4 font-semibold ${pathname === `/tournament/${id}/games` ? 'border-b-2 border-primary text-primary' : 'text-muted hover:text-primary'}`}
        >
        Matchs
        </Link>
        <Link
          href={`/tournament/${id}/results`}
          className={`pb-2 px-4 font-semibold ${pathname === `/tournament/${id}/results` ? 'border-b-2 border-primary text-primary' : 'text-muted hover:text-primary'}`}
        >
          Tableau
        </Link>
      </div>

      {/* Affichage du contenu spécifique de chaque onglet */}
      <div>{children}</div>

      <ToastContainer />
    </div>
  );
}