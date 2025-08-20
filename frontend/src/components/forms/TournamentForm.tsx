'use client';

import { FormEvent } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { Trophy } from 'lucide-react';
import TournamentInfoSection from '@/src/components/forms/TournamentInfoSection';
import TournamentDatesSection from '@/src/components/forms/TournamentDatesSection';
import TournamentConfigSection from '@/src/components/forms/TournamentConfigSection';
import type { Tournament } from '@/src/types/tournament';
import { buildTournamentPayload } from '@/src/validation/tournament';
import type { TournamentPayload } from '@/src/validation/tournament';
import { useTournamentForm } from '@/src/hooks/useTournamentForm';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import { useRouter } from 'next/navigation';

interface TournamentFormProps {
  initialData?: Partial<Tournament>;
  onSubmit: (data: TournamentPayload) => Promise<void>;
  isEditing?: boolean;
  title?: string;
}

export default function TournamentForm({ initialData, onSubmit, isEditing = false, title }: TournamentFormProps) {
  const { formData, isSubmitting, setIsSubmitting, handleInputChange, validate } = useTournamentForm(initialData);
  const router = useRouter();

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
      const payload = buildTournamentPayload(parsed.data);
      await onSubmit(payload);

      if (payload.id) {
        router.push(`/admin/tournament/${payload.id}/players`);
      }
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
        <div className="fixed inset-0 z-[2000] flex items-center justify-center bg-background/60">
          <CenteredLoader size={48} />
        </div>
      )}
      <div className="container mx-auto max-w-4xl pb-[calc(var(--bottom-nav-height,64px)+96px)]" aria-busy={isSubmitting}>
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
            <form id="tournament-form" onSubmit={handleSubmit}>
              <TournamentInfoSection formData={formData} handleInputChange={handleInputChange} />

              <hr className="border-border" />

              <TournamentDatesSection formData={formData} handleInputChange={handleInputChange} />

              <hr className="border-border" />

              <TournamentConfigSection formData={formData} handleInputChange={handleInputChange} />

              <hr className="border-border" />
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
      {/* Floating submit button (always visible) */}
      <button
        type="submit"
        form="tournament-form"
        disabled={isSubmitting}
        className="fixed inset-x-0 z-[1000] flex items-center justify-center gap-2 px-5 py-4 shadow-lg bg-primary text-on-primary hover:bg-primary-hover focus:outline-none focus:ring-2 focus:ring-primary/40"
        style={{ bottom: 'calc(env(safe-area-inset-bottom) + var(--bottom-nav-height, 64px) + 12px)' }}
        aria-busy={isSubmitting}
        aria-label={isEditing ? (isSubmitting ? 'Mise à jour…' : 'Mettre à jour') : (isSubmitting ? 'Création en cours…' : 'Créer le tournoi')}
      >
        <Trophy className="h-5 w-5" />
        <span className="text-base font-medium">
          {isEditing ? 'Mettre à jour' : 'Créer le tournoi'}
        </span>
      </button>
    </>
  );
}