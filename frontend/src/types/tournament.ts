import React, { useState } from 'react';
import { Tournament } from '@/src/types/tournament';

// @todo duplicate of Tournament, to remove ?
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

