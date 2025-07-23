import { useRouter } from 'next/navigation';
import TournamentForm from './TournamentForm';
import { toast } from 'react-toastify';

export default function CreateTournamentForm() {
  const router = useRouter();

  const handleCreate = async (data) => {
    const res = await fetch('http://localhost:8080/tournaments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error();
    const tournament = await res.json();
    toast.success('Tournoi créé !');
    router.push(`/admin/tournament/${tournament.id}`);
  };

  return <TournamentForm onSubmit={handleCreate} />;
}