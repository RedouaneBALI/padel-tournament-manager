'use client';

import { use } from 'react';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import TournamentForm from '@/components/forms/TournamentForm';
import { toast } from 'react-toastify';
import { Loader2, Pencil } from 'lucide-react';

export default function EditTournamentPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  const [initialData, setInitialData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetch(`http://localhost:8080/tournaments/${id}`)
      .then((res) => res.json())
      .then((data) => {
        setInitialData(data);
        setIsLoading(false);
      })
      .catch(() => {
        toast.error("Erreur lors du chargement du tournoi");
        setIsLoading(false);
      });
  }, [id]);

  const handleUpdate = async (data: any) => {
    const res = await fetch(`http://localhost:8080/tournaments/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });

    if (!res.ok) {
      toast.error("Échec de la mise à jour.");
      return;
    }

    toast.success('Tournoi mis à jour !');
    router.push(`/admin/tournament/${id}`);
  };

  if (isLoading || !initialData) {
    return (
      <div className="flex justify-center items-center h-96">
        <Loader2 className="h-6 w-6 animate-spin text-primary" />
        <span className="ml-2 text-muted-foreground">Chargement...</span>
      </div>
    );
  }

  return (
    <div className="container mx-auto max-w-4xl p-6">
      <div className="bg-card rounded-lg shadow-lg border border-border">
        <div className="p-6 border-b border-border flex items-center gap-3">
          <div className="p-2 rounded-lg bg-primary/10">
            <Pencil className="h-6 w-6 text-primary" />
          </div>
          <h1 className="text-2xl font-semibold text-card-foreground">
            Modifier le tournoi
          </h1>
        </div>

        <div className="p-6">
          <TournamentForm
            initialData={initialData}
            onSubmit={handleUpdate}
            isEditing={true}
          />
        </div>
      </div>
    </div>
  );
}