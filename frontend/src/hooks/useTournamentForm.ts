'use client';

import { useEffect, useState } from 'react';
import type { Tournament } from '@/src/types/tournament';
import { getGroupsConfig } from '@/src/types/tournament';
import { TournamentFormSchema, type TournamentFormData } from '@/src/validation/tournament';

export function getInitialFormData(initialData?: Partial<Tournament>): TournamentFormData {
  const tournamentFormat = initialData?.tournamentFormat ?? 'KNOCKOUT';

  const defaults: TournamentFormData = TournamentFormSchema.parse({});

  // Hydrate defaults from existing Groups+KO config if present
  if (tournamentFormat === 'GROUPS_KO') {
    const cfg = getGroupsConfig(initialData as Tournament | undefined);
    if (cfg) {
      defaults.nbPools = cfg.nbPools ?? defaults.nbPools;
      defaults.nbPairsPerPool = cfg.nbPairsPerPool ?? defaults.nbPairsPerPool;
      defaults.nbQualifiedByPool = cfg.nbQualifiedByPool ?? defaults.nbQualifiedByPool;
      defaults.nbSeeds = cfg.nbSeeds ?? defaults.nbSeeds;
    }
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
    if (!initialData) return;
    if (initialData.tournamentFormat === 'GROUPS_KO') {
      const cfg = getGroupsConfig(initialData as Tournament | undefined);
      setFormData((prev) => ({
        ...prev,
        ...initialData,
        nbPools: cfg?.nbPools ?? prev.nbPools,
        nbPairsPerPool: cfg?.nbPairsPerPool ?? prev.nbPairsPerPool,
        nbQualifiedByPool: cfg?.nbQualifiedByPool ?? prev.nbQualifiedByPool,
        nbSeeds: cfg?.nbSeeds ?? prev.nbSeeds,
      }) as TournamentFormData);
    } else {
      setFormData((prev) => ({ ...prev, ...initialData } as TournamentFormData));
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
        type FormFormat = TournamentFormData['tournamentFormat'];
        const isGroups = value === 'GROUPS_KO';
        const normalized: FormFormat = value as FormFormat; // value is either 'KNOCKOUT' or 'GROUPS_KO'
        const next: TournamentFormData = { ...prev, tournamentFormat: normalized };
        if (isGroups && !nbSeedsTouched && !groupDefaultApplied) {
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
