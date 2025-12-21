import { createContext, useContext } from 'react';

export interface TournamentContextData {
  name: string | null;
  club: string | null;
  level: string | null;
}

export const TournamentContext = createContext<TournamentContextData | null>(null);

export const useTournament = () => {
  const tournament = useContext(TournamentContext);
  return tournament;
};
