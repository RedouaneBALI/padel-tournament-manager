export type TournamentFormData = {
  id?: number | null;
  name: string | null;
  description: string | null;
  city: string | null;
  club: string | null;
  gender: string | null;
  level: string | null;
  tournamentFormat: string | null;
  nbSeeds: 16;
  startDate: string | null;
  endDate: string | null;
  nbMaxPairs: 48;
};