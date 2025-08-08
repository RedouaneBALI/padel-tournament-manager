'use client';

import { useState } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { Loader2, Trophy } from 'lucide-react';
import TournamentInfoSection from '@/src/components/tournament/TournamentInfoSection';
import TournamentDatesSection from '@/src/components/tournament/TournamentDatesSection';
import TournamentConfigSection from '@/src/components/tournament/TournamentConfigSection';
import { Tournament } from '@/src/types/tournament';
import { TournamentFormData } from '@/src/types/tournamentData';

const getInitialFormData = (initialData?: Partial<Tournament>): TournamentFormData => ({
  name: '',
  description: '',
  city: '',
  club: '',
  gender: '',
  level: '',
  tournamentFormat: 'KNOCKOUT',
  nbSeeds: 16,
  startDate: '',
  endDate: '',
  nbMaxPairs: 32,
  nbPools: 3,
  nbPairsPerPool: 3,
  nbQualifiedByPool: 2,
  ...initialData,
});

interface TournamentFormProps {
  initialData?: Partial<Tournament>;
  onSubmit: (data: Tournament) => Promise<void>;
  isEditing?: boolean;
  title?: string;
}


export default function TournamentForm({ initialData, onSubmit, isEditing = false, title }: TournamentFormProps) {
  const [formData, setFormData] = useState<TournamentFormData>(getInitialFormData(initialData));

  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev: TournamentFormData) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    const payload: TournamentFormData = { ...formData };

    if (payload.nbSeeds) {
      payload.nbSeeds = parseInt(String(payload.nbSeeds), 10);
    }

    Object.keys(payload).forEach(key => {
      const typedKey = key as keyof TournamentFormData;
      if (payload[typedKey] === '') {
        payload[typedKey] = null;
      }
    });

    try {
      const tournament = payload as Tournament;
      await onSubmit(tournament);
    } catch (error) {
      console.error(error);
      toast.error("Une erreur s'est produite lors de l'opération.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="container mx-auto max-w-4xl">
      <div className="bg-card shadow-lg border border-border">
        <div className="p-6">
          {title && (
            <div className="flex items-center gap-3">
              <div className="p-2 bg-primary/10">
                <Trophy className="h-6 w-6 text-primary" />
              </div>
              <h1 className="text-2xl font-semibold text-card-foreground">{title}</h1>
            </div>
          )}
        </div>

        <div>
          <form onSubmit={handleSubmit} className="space-y-8">
            <TournamentInfoSection
              formData={formData}
              handleInputChange={handleInputChange}
            />

            <hr className="border-border" />

            <TournamentDatesSection
              formData={formData}
              handleInputChange={handleInputChange}
            />

            <hr className="border-border" />

            <TournamentConfigSection
              formData={formData}
              handleInputChange={handleInputChange}
            />

            <hr className="border-border" />

            <div className="flex justify-end p-4">
              <button
                type="submit"
                disabled={isSubmitting || !formData.name}
                className="px-4 py-2 bg-[#1b2d5e] text-white rounded hover:bg-blue-900"
              >
                {isSubmitting && <Loader2 className="h-4 w-4 animate-spin" />}
                {isSubmitting
                  ? (isEditing ? 'Mise à jour...' : 'Création en cours...')
                  : (isEditing ? 'Mettre à jour' : 'Créer le tournoi')
                }
              </button>
            </div>
          </form>
        </div>
      </div>
      <ToastContainer
        position="top-right"
        autoClose={5000}
        hideProgressBar={false}
        newestOnTop={false}
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
      />
    </div>
  );
}