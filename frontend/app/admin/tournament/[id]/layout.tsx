'use client';

import React, { useEffect, useState, use } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import Link from 'next/link';
import { ToastContainer, toast } from 'react-toastify';
import { Settings, Share2 } from 'lucide-react';
import type { Tournament } from '@/src/types/tournament';

export default function AdminTournamentLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const pathname = usePathname();
  const router = useRouter();

  const [tournament, setTournament] = useState<Tournament | null>(null);

  useEffect(() => {
    async function fetchTournament() {
      try {
        const res = await fetch(`http://localhost:8080/tournaments/${id}`);
        if (!res.ok) throw new Error();
        const data = await res.json();
        setTournament(data);
        console.log("data : ");
        console.log(data);
      } catch (err) {
        toast.error('Erreur lors du chargement du tournoi : ' + err);
      }
    }

    fetchTournament();
  }, [id]);

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">
          {tournament?.name ?? 'Chargement...'}
        </h1>
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              const shareUrl = `http://localhost:3000/tournament/${id}`;
              navigator.clipboard
                .writeText(shareUrl)
                .then(() => toast.success('Lien copié dans le presse-papiers !'))
                .catch(() => prompt('Copiez ce lien :', shareUrl));
            }}
            className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
            title="Partager le lien aux joueurs"
          >
            <Share2 className="h-5 w-5 text-muted-foreground hover:text-primary" />
          </button>

          <button
            onClick={() => router.push(`/admin/tournament/${id}/edit`)}
            className="p-2 rounded hover:bg-muted transition-colors cursor-pointer"
            title="Modifier le tournoi"
          >
            <Settings className="h-5 w-5 text-muted-foreground hover:text-primary" />
          </button>
        </div>
      </div>

      <div className="flex justify-center mb-6 space-x-4 border-b">
        <Link
          href={`/admin/tournament/${id}/players`}
          className={`pb-2 px-4 font-semibold ${
            pathname.endsWith('/players')
              ? 'border-b-2 border-[#1b2d5e] text-primary'
              : 'text-gray-500 hover:text-primary'
          }`}
        >
          Joueurs
        </Link>
        <Link
          href={`/admin/tournament/${id}/rounds/config`}
          className={`pb-2 px-4 font-semibold ${
            pathname.includes('/rounds/config')
              ? 'border-b-2 border-[#1b2d5e] text-primary'
              : 'text-gray-500 hover:text-primary'
          }`}
        >
          Format
        </Link>
        <Link
          href={`/admin/tournament/${id}/rounds/results`}
          className={`pb-2 px-4 font-semibold ${
            pathname.includes('/rounds/results')
              ? 'border-b-2 border-[#1b2d5e] text-primary'
              : 'text-gray-500 hover:text-primary'
          }`}
        >
          Tableau
        </Link>
      </div>

      <div className="mt-6">
        {children}
      </div>

      <ToastContainer />
    </div>
  );
}