// app/admin/tournament/[id]/rounds/config/page.tsx
import React from 'react';
import RoundFormatTab from '@/src/components/round/RoundFormatTab';
import { headers } from 'next/headers';

type Props = {
  params: Promise<{
    id: string;
  }>;
};

export default async function MatchFormatConfigPage({ params }: Props) {
  const { id } = await params;
  const hdrs = headers();
  const proto = hdrs.get('x-forwarded-proto') ?? 'http';
  const host = hdrs.get('host');
  const origin = host ? `${proto}://${host}` : process.env.NEXT_PUBLIC_BASE_URL ?? 'http://localhost:3000';

  const res = await fetch(`${origin}/api/tournaments/${id}/pairs`, { cache: 'no-store' });
  if (!res.ok) {
    // Keep a graceful empty state rather than crashing the server component
    return <RoundFormatTab tournamentId={id} pairs={[]} />;
  }
  const pairs = await res.json();
  return <RoundFormatTab tournamentId={id} pairs={pairs} />;
}
