import { Tournament } from '@/app/types/Tournament';

interface TournamentOverviewTabProps {
  tournament: Tournament;
}

export default function TournamentOverviewTab({ tournament }: TournamentOverviewTabProps) {
  return (
    <section className="bg-white p-6 rounded-md shadow-md">
      <h2 className="text-xl font-semibold mb-4">Informations générales</h2>
      <p><strong>Nom :</strong> {tournament.name}</p>
      <p><strong>Description :</strong> {tournament.description || '—'}</p>
      <p><strong>Ville :</strong> {tournament.city || '—'}</p>
      <p><strong>Club :</strong> {tournament.club || '—'}</p>
      <p><strong>Genre :</strong> {tournament.gender || '—'}</p>
      <p><strong>Niveau :</strong> {tournament.level || '—'}</p>
      <p><strong>Format :</strong> {tournament.tournamentFormat || '—'}</p>
      <p><strong>Nombre de têtes de série :</strong> {tournament.nbSeeds}</p>
      <p><strong>Date de début :</strong> {tournament.startDate || '—'}</p>
      <p><strong>Date de fin :</strong> {tournament.endDate || '—'}</p>
    </section>
  );
}