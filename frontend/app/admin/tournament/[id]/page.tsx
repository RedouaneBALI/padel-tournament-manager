import { redirect } from 'next/navigation';

export default function Page({ params }: { params: { id: string }}) {
  redirect(`/admin/tournament/${params.id}/players`);
}