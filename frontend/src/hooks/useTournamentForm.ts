'use client';

import { useEffect, useState } from 'react';
import { Tournament } from '@/src/types/tournament';
import type { TournamentFormData } from '@/src/validation/tournament';
import { TournamentFormSchema } from '@/src/validation/tournament';

export function getInitialFormData(initialData?: Partial<TournamentFormData>): TournamentFormData {
  const defaults: TournamentFormData = {
    id: null,
    name: '',
    description: '',
    city: '',
    club: '',
    gender: null,
    level: null,
    startDate: null,
    endDate: null,
    config: {
      format: 'KNOCKOUT',
      mainDrawSize: 32,
      nbSeeds: 8,
      nbPools: 4,
      nbPairsPerPool: 4,
      nbQualifiedByPool: 2,
      preQualDrawSize: 16,
      nbQualifiers: 4,
      drawMode: "SEEDED",
    },
  };

  return {
    ...defaults,
    ...initialData,
    config: {
      ...defaults.config,
      ...initialData?.config,
    },
  };
}

export function useTournamentForm(initialData?: Partial<TournamentFormData>) {
  const [formData, setFormData] = useState<TournamentFormData>(getInitialFormData(initialData));
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!initialData) return;
    setFormData((prev) => ({
      ...prev,
      ...initialData,
      config: {
        ...prev.config,
        ...initialData.config,
      },
    }));
  }, [initialData]);

  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;

    setFormData((prev) => {
      const updated = { ...prev } as TournamentFormData;

      if (name.startsWith('config.')) {
        const key = name.split('.')[1] as keyof TournamentFormData['config'];
        const numericKeys = new Set([
          'mainDrawSize', 'nbSeeds', 'nbPools', 'nbPairsPerPool', 'nbQualifiedByPool',
          'preQualDrawSize', 'nbQualifiers', 'nbSeedsQualify',
        ]);

        updated.config = { ...prev.config } as TournamentFormData['config'];
        if (key === 'drawMode') {
          (updated.config as any)[key] = value; // 'SEEDED' | 'MANUAL'
        } else if (numericKeys.has(key as string)) {
          (updated.config as any)[key] = value === '' ? null : parseInt(value, 10);
        } else {
          (updated.config as any)[key] = value as any;
        }
      } else {
        (updated as any)[name] = value;
      }

      return updated;
    });
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