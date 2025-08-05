import React, { useState } from 'react';
import { Tournament } from '@/src/types/tournament';

interface TournamentFormData {
  name: string;
  description: string;
  city: string;
  club: string;
  gender: string;
  level: string;
  tournamentFormat: string;
  nbSeeds: number;
  startDate: string;
  endDate: string;
  nbMaxPairs: number;
  nbPools: number;
  nbPairsPerPool: number;
  nbQualifiedByPool: number;
}

const getInitialFormData = (initialData?: Partial<Tournament>): TournamentFormData => ({
  name: '',
  description: '',
  city: '',
  club: '',
  gender: '',
  level: '',
  tournamentFormat: '',
  nbSeeds: 16,
  startDate: '',
  endDate: '',
  nbMaxPairs: 48,
  nbPools: 4,
  nbPairsPerPool: 4,
  nbQualifiedByPool: 2,
  ...initialData,
});

const TournamentForm: React.FC<{ initialData?: Partial<Tournament> }> = ({ initialData }) => {
  const [formData, setFormData] = useState<TournamentFormData>(getInitialFormData(initialData));

  // ...rest of the component code
};
