import React from 'react';
import { Tournament } from '@/src/types/tournament';
import FavoriteTournamentCard from '@/src/components/tournament/FavoriteTournamentCard';

interface FavoriteTournamentsListProps {
  favoriteTournaments: Tournament[];
}

const FavoriteTournamentsList: React.FC<FavoriteTournamentsListProps> = ({
  favoriteTournaments,
}) => {
  return (
    <div>
      {favoriteTournaments.length === 0 ? (
        <p className="text-muted-foreground px-4">Aucun tournoi favori.</p>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {favoriteTournaments.map((tournament) => (
            <FavoriteTournamentCard key={tournament.id} tournament={tournament} />
          ))}
        </div>
      )}
    </div>
  );
};

export default FavoriteTournamentsList;
