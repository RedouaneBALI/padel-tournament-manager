import React from "react";
import { Tournament } from "@/src/types/tournament";
import Link from "next/link";
import { Trophy } from "lucide-react";

interface FavoriteTournamentCardProps {
  tournament: Tournament;
}

const FavoriteTournamentCard: React.FC<FavoriteTournamentCardProps> = ({
  tournament,
}) => {
  return (
    <Link
      href={`/tournament/${tournament.id}`}
      className="block bg-card border border-border rounded-2xl shadow-md overflow-hidden hover:bg-accent transition-colors"
    >
      {/* Header */}
      <div className="p-4 border-b border-border">
        <div className="flex items-center gap-2">
          <Trophy className="h-5 w-5 text-primary" />
          <h3 className="text-lg font-semibold text-card-foreground truncate">
            {tournament.name || 'Tournoi sans nom'}
          </h3>
        </div>
        <p className="text-sm text-text-secondary mt-1">{tournament.club} - {tournament.level}</p>
      </div>
    </Link>
  );
};

export default FavoriteTournamentCard;
