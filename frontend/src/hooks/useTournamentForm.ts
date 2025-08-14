'use client';

import { useEffect, useState } from 'react';
import type { TournamentFormData } from '@/src/validation/tournament';
import { TournamentFormSchema } from '@/src/validation/tournament';

export function getInitialFormData(initialData?: Partial<Tournament>): TournamentFormData {
  const tournamentFormat = initialData?.tournamentFormat ?? 'KNOCKOUT';

  const defaults: TournamentFormData = TournamentFormSchema.parse({});

  if (!initialData?.nbSeeds && tournamentFormat === 'GROUP_STAGE') {
    defaults.nbSeeds = initialData?.nbPools ?? defaults.nbPools;
  }

  return {
    ...defaults,
    ...initialData,
  } as TournamentFormData;
}

export function useTournamentForm(initialData?: Partial<Tournament>) {
  const [formData, setFormData] = useState<TournamentFormData>(getInitialFormData(initialData));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [nbSeedsTouched, setNbSeedsTouched] = useState(false);
  const [groupDefaultApplied, setGroupDefaultApplied] = useState(false);

  useEffect(() => {
    if (initialData) {
      setFormData((prev) => ({ ...prev, ...initialData }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialData]);

  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;

    if (name === 'nbSeeds') setNbSeedsTouched(true);

    if (name === 'tournamentFormat') {
      setFormData((prev) => {
        const next: TournamentFormData = { ...prev, tournamentFormat: value as any };
        if (value === 'GROUP_STAGE' && !nbSeedsTouched && !groupDefaultApplied) {
          next.nbSeeds = prev.nbPools ?? 3;
          setGroupDefaultApplied(true);
        }
        if (value === 'KNOCKOUT') {
          next.nbSeeds = 16;
          setGroupDefaultApplied(false);
        }
        return next;
      });
      return;
    }

    setFormData((prev) => ({ ...prev, [name]: value }) as TournamentFormData);
  };

  const validate = () => TournamentFormSchema.safeParse(formData);

  return {
    formData,
    setFormData,
    isSubmitting,
    setIsSubmitting,
    handleInputChange,
    validate,
  };
}
