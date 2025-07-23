'use client';

import { useState } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { Loader2, Trophy } from 'lucide-react';
import TournamentInfoSection from '@/components/tournament/TournamentInfoSection';
import TournamentDatesSection from '@/components/tournament/TournamentDatesSection';
import TournamentConfigSection from '@/components/tournament/TournamentConfigSection';

interface TournamentFormProps {
  initialData?: any;
  onSubmit: (data: any) => Promise<void>;
  isEditing?: boolean;
  title?: string;
}

export default function TournamentForm({ initialData, onSubmit, isEditing = false, title }: TournamentFormProps) {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    city: '',
    club: '',
    gender: '',
    level: '',
    tournamentFormat: '',
    nbSeeds: '',
    startDate: '',
    endDate: '',
    ...initialData,
  });

  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    const payload = { ...formData };
    if (payload.nbSeeds) {
      payload.nbSeeds = parseInt(payload.nbSeeds, 10);
    }

    Object.keys(payload).forEach(key => {
      if (payload[key] === '') payload[key] = null;
    });

    try {
      await onSubmit(payload);
      toast.success(isEditing ? "Tournoi mis à jour avec succès!" : "Tournoi créé avec succès!");
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

            <div className="flex justify-end pt-4">
              <button
                type="submit"
                disabled={isSubmitting || !formData.name}
                className="inline-flex items-center gap-2 px-6 py-3 bg-primary text-primary-foreground font-medium rounded-md shadow-sm hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors min-w-[200px] justify-center"
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