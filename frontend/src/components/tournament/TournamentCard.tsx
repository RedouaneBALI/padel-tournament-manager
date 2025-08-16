import React from "react";
import { Tournament } from "@/src/types/tournament";
import Link from "next/link";
import { Trophy, Settings, Trash2 } from "lucide-react";

interface TournamentCardProps {
  tournament: Tournament;
  onDelete: (id: string) => void;
  isDeleting: boolean;
}

const TournamentCard: React.FC<TournamentCardProps> = ({
  tournament,
  onDelete,
  isDeleting,
}) => {
  return (
    <div className="bg-card border border-border rounded-2xl shadow-md overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b border-border">
        <div className="flex items-center gap-2">
          <Trophy className="h-5 w-5 text-primary" />
          <h3 className="text-lg font-semibold text-card-foreground truncate">
            {tournament.name || 'Tournoi sans nom'}
          </h3>
        </div>
        <p className="text-sm text-text-secondary mt-1">{tournament.playerPairs.length} équipes - {tournament.rounds.length} rounds</p>
      </div>

      {/* Footer */}
      <div className="p-4 border-t border-border flex items-center justify-end gap-3">
        <Link
          href={`/admin/tournament/${tournament.id}`}
          className="inline-flex items-center gap-2 rounded-md bg-primary text-on-primary hover:bg-primary-hover px-3 py-2 text-sm"
          aria-label="Gérer le tournoi"
          title="Gérer le tournoi"
        >
          <Settings className="h-4 w-4" />
          <span>Gérer</span>
        </Link>

        <button
          onClick={() => onDelete(String(tournament.id))}
          disabled={isDeleting}
          aria-label="Supprimer le tournoi"
          title="Supprimer le tournoi"
          className="inline-flex items-center gap-2 rounded-md border border-error text-error hover:bg-error/10 px-3 py-2 text-sm disabled:opacity-60 disabled:cursor-not-allowed"
        >
          <Trash2 className="h-4 w-4" />
          <span>Supprimer</span>
        </button>
      </div>
    </div>
  );
};

export default TournamentCard;