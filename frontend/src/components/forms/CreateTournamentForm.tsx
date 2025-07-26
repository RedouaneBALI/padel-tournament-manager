import { useRouter } from 'next/navigation';
import TournamentForm from '@/src/components/forms/TournamentForm';
import { toast } from 'react-toastify';
import { Tournament } from '@/src/types/tournament';

export default function CreateTournamentForm() {
  const router = useRouter();

  const handleCreate = async (data: Tournament) => {
    const res = await fetch('http://localhost:8080/tournaments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!res.ok) throw new Error();
    const tournament = await res.json();
    toast.success('Tournoi créé !');

    setTimeout(() => {
      router.push(`/admin/tournament/${tournament.id}/edit`);
    }, 300); // 300 ms suffisent souvent

  return <TournamentForm onSubmit={handleCreate} />;
}