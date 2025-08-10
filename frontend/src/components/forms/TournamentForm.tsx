'use client';

import { useState, useEffect } from 'react';
import { toast, ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import { Loader2, Trophy } from 'lucide-react';
import TournamentInfoSection from '@/src/components/tournament/TournamentInfoSection';
import TournamentDatesSection from '@/src/components/tournament/TournamentDatesSection';
import TournamentConfigSection from '@/src/components/tournament/TournamentConfigSection';
import { Tournament } from '@/src/types/tournament';
import { TournamentFormData } from '@/src/types/tournamentData';

const getNumber = (v: unknown, fallback = 0): number => {
  const n = typeof v === 'string' ? parseInt(v, 10) : (typeof v === 'number' ? v : NaN);
  return Number.isFinite(n) ? n : fallback;
};

const getValidationErrors = (data: TournamentFormData): string[] => {
  const errors: string[] = [];
  const format = data.tournamentFormat ?? 'KNOCKOUT';

  const nbSeeds = getNumber(data.nbSeeds, 0);
  const nbMaxPairs = getNumber(data.nbMaxPairs, 0);
  const nbPools = getNumber(data.nbPools, 0);
  const nbPairsPerPool = getNumber(data.nbPairsPerPool, 0);

  const name = (data.name ?? '').trim();
  if (!name) {
    errors.push('Le tournoi doit avoir un nom.');
  }

  if (format === 'KNOCKOUT') {
    const maxSeeds = Math.floor(nbMaxPairs / 2);
    if (nbMaxPairs < 4 || nbMaxPairs > 128) {
      errors.push(`En élimination directe, le nombre d'équipes maximum doit être compris entre 4 et 128 (actuellement ${nbMaxPairs}).`);
    }
    if (nbSeeds > maxSeeds) {
      errors.push(`En élimination directe, le nombre de têtes de série (${nbSeeds}) ne peut pas dépasser la moitié du nombre d'équipes (${nbMaxPairs} → max ${maxSeeds}).`);
    }
  }

  if (format === 'GROUP_STAGE') {
    if (nbPairsPerPool < 3) {
      errors.push(`En phase de poules, il doit y avoir au moins 3 équipes par poule (actuellement ${nbPairsPerPool}).`);
    }
    const maxSeeds = nbPools * nbPairsPerPool;
    if (nbSeeds > maxSeeds) {
      errors.push(`En phase de poules, le nombre de têtes de série (${nbSeeds}) ne peut pas dépasser le total d'équipes (${nbPools}×${nbPairsPerPool} = ${maxSeeds}).`);
    }
  }

  return errors;
};

const getInitialFormData = (initialData?: Partial<Tournament>): TournamentFormData => {
  const tournamentFormat = initialData?.tournamentFormat ?? 'KNOCKOUT';

  // Defaults for creation
  const defaults: TournamentFormData = {
    name: '',
    description: '',
    city: '',
    club: '',
    gender: '',
    level: '',
    tournamentFormat,
    nbSeeds: 16,
    startDate: '',
    endDate: '',
    nbMaxPairs: 32,
    nbPools: 4,
    nbPairsPerPool: 4,
    nbQualifiedByPool: 2,
  };

  // If format is GROUP_STAGE at mount and nbSeeds not provided, default nbSeeds to nbPools
  if (!initialData?.nbSeeds && tournamentFormat === 'GROUP_STAGE') {
    defaults.nbSeeds = (initialData?.nbPools ?? defaults.nbPools);
  }

  return {
    ...defaults,
    ...initialData,
  };
};

interface TournamentFormProps {
  initialData?: Partial<Tournament>;
  onSubmit: (data: Tournament) => Promise<void>;
  isEditing?: boolean;
  title?: string;
}


export default function TournamentForm({ initialData, onSubmit, isEditing = false, title }: TournamentFormProps) {
  const [formData, setFormData] = useState<TournamentFormData>(getInitialFormData(initialData));

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [nbSeedsTouched, setNbSeedsTouched] = useState(false);
  const [groupDefaultApplied, setGroupDefaultApplied] = useState(false);

  /** If a parent later passes initialData (edit page), hydrate once */
  useEffect(() => {
    if (initialData) {
      setFormData(prev => ({ ...prev, ...initialData }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialData]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    if (name === 'nbSeeds') {
      setNbSeedsTouched(true);
    }

    if (name === 'tournamentFormat') {
      setFormData(prev => {
        const next: TournamentFormData = { ...prev, tournamentFormat: value as any };
        if (value === 'GROUP_STAGE' && !nbSeedsTouched && !groupDefaultApplied) {
          next.nbSeeds = prev.nbPools ?? 3;
          setGroupDefaultApplied(true);
        }
        // Always reset nbSeeds to 16 when switching to KNOCKOUT
        if (value === 'KNOCKOUT') {
          next.nbSeeds = 16;
          setGroupDefaultApplied(false);
        }

        return next;
      });
      return;
    }

    setFormData((prev: TournamentFormData) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    const errors = getValidationErrors(formData);
    if (errors.length > 0) {
      toast.error(errors.join('\n'));
      setIsSubmitting(false);
      return;
    }

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

    const tournament = payload as Tournament;
    await onSubmit(tournament);
    setIsSubmitting(false);
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
                disabled={isSubmitting}
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