'use client';

import { FormEvent } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { Trophy } from 'lucide-react';
import TournamentInfoSection from '@/src/components/forms/TournamentInfoSection';
import TournamentDatesSection from '@/src/components/forms/TournamentDatesSection';
import TournamentConfigSection from '@/src/components/forms/TournamentConfigSection';
import type { Tournament } from '@/src/types/tournament';
import type { ParsedTournamentForm } from '@/src/validation/tournament';
import { useTournamentForm } from '@/src/hooks/useTournamentForm';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import { useRouter } from 'next/navigation';
import type { TournamentFormData } from '@/src/validation/tournament';
import Button from '@/src/components/ui/buttons/Button';

interface TournamentFormProps {
  initialData?: Partial<Tournament>;
  onSubmit: (data: ParsedTournamentForm) => Promise<void>;
  isEditing?: boolean;
  title?: string;
}

export default function TournamentForm({
  initialData,
  onSubmit,
  isEditing = false,
  title,
}: TournamentFormProps) {
  const { formData, isSubmitting, setIsSubmitting, handleInputChange, handleEmailsChange, validate } =
    useTournamentForm(initialData as Partial<TournamentFormData>);
  const router = useRouter();

  // Déterminer si le tournoi est lancé (a des rounds ou un currentRoundStage)
  const isTournamentStarted = isEditing && initialData && (
    (initialData.rounds && initialData.rounds.length > 0) ||
    (initialData.currentRoundStage && initialData.currentRoundStage !== null)
  );

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
      await onSubmit(parsed.data);
      if (parsed.data.id) {
        router.push(`/admin/tournament/${parsed.data.id}/players`);
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
      <div
        className="container mx-auto max-w-4xl pb-[calc(var(--bottom-nav-height,64px)+96px)]"
        aria-busy={isSubmitting}
      >
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

              <TournamentConfigSection
                formData={formData}
                handleInputChange={handleInputChange}
                onEmailsChange={handleEmailsChange}
                isTournamentStarted={isTournamentStarted}
              />

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
      <Button
        type="submit"
        form="tournament-form"
        onClick={(e) => {
          // Si le bouton est déjà rattaché au form via l'attribut `form`, pas besoin du fallback.
          const btn = e?.currentTarget;
          // event.currentTarget.form est le form natif lié au bouton (peut être null)
          const nativeFormFromButton = btn?.form as HTMLFormElement | null | undefined;
          if (nativeFormFromButton) {
            // L'attribut form est actif dans cet environnement : laisse le navigateur gérer la soumission
            return;
          }

          // Fallback global : tente de récupérer le form par id
          const formEl = document.getElementById('tournament-form') as HTMLFormElement | null;
          if (formEl) {
            if (typeof formEl.requestSubmit === 'function') {
              formEl.requestSubmit();
            } else {
              // requestSubmit non disponible : créer un bouton submit temporaire et le cliquer
              const tempBtn = document.createElement('button');
              tempBtn.type = 'submit';
              tempBtn.style.display = 'none';
              formEl.appendChild(tempBtn);
              tempBtn.click();
              tempBtn.remove();
            }
          }
        }}
        aria-label={
          isEditing
            ? isSubmitting
              ? 'Mise à jour…'
              : 'Mettre à jour'
            : isSubmitting
            ? 'Création en cours…'
            : 'Créer le tournoi'
        }
        className={`fixed inset-x-0 z-[1000] flex items-center justify-center gap-2 px-5 py-4 shadow-lg floating-submit`}
        disabled={isSubmitting}

      >
        <Trophy className="h-5 w-5" />
        <span className="text-base font-medium">
          {isEditing ? 'Mettre à jour' : 'Créer le tournoi'}
        </span>
      </Button>
    </>
  );
}