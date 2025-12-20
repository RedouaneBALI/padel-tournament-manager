import { createContext, useContext } from 'react';

export const TournamentNameContext = createContext<string | null>(null);

export const useTournamentName = () => {
  const name = useContext(TournamentNameContext);
  return name;
};

