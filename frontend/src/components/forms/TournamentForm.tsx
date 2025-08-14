'use client';

import { FormEvent } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { Trophy } from 'lucide-react';
import TournamentInfoSection from '@/src/components/forms/TournamentInfoSection';
import TournamentDatesSection from '@/src/components/forms/TournamentDatesSection';
import TournamentConfigSection from '@/src/components/forms/TournamentConfigSection';
import type { Tournament } from '@/src/types/tournament';
import { useTournamentForm } from '@/src/hooks/useTournamentForm';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

interface TournamentFormProps {
  initialData?: Partial<Tournament>;
  onSubmit: (data: Tournament) => Promise<void>;
  isEditing?: boolean;
  title?: string;
}

export default function TournamentForm({ initialData, onSubmit, isEditing = false, title }: TournamentFormProps) {
  const { formData, isSubmitting, setIsSubmitting, handleInputChange, validate } = useTournamentForm(initialData);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const parsed = validate();
      if (!parsed.success) {
        const messages = parsed.error.issues.map((i) => i.message);
        toast.error(messages.join('\n'));
        return;
      }
      const validData = parsed.data as unknown as Tournament;
      await onSubmit(validData);
    } catch (err: any) {
      const msg = err?.message ?? 'Erreur inconnue lors de la création du tournoi.';
      toast.error(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      {isSubmitting && (
<CenteredLoader />
      )}
      <div className="container mx-auto max-w-4xl" aria-busy={isSubmitting}>
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
            <form onSubmit={handleSubmit}>
              <TournamentInfoSection formData={formData} handleInputChange={handleInputChange} />

              <hr className="border-border" />

              <TournamentDatesSection formData={formData} handleInputChange={handleInputChange} />

              <hr className="border-border" />

              <TournamentConfigSection formData={formData} handleInputChange={handleInputChange} />

              <hr className="border-border" />

              <div className="p-4">
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full px-4 py-2 bg-primary text-on-primary rounded hover:bg-primary-hover"
                >
                  {isSubmitting
                    ? isEditing
                      ? 'Mise à jour...'
                      : 'Création en cours...'
                    : isEditing
                    ? 'Mettre à jour'
                    : 'Créer le tournoi'}
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
    </>
  );
}