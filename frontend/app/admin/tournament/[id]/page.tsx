import { redirect } from 'next/navigation';
import { use } from 'react';

export default function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  redirect(`/admin/tournament/${id}/players`);
}