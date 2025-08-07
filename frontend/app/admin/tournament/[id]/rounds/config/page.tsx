import React from 'react';
import RoundFormatTab from '@/src/components/round/RoundFormatTab';
import { fetchPairs } from '@/src/api/tournamentApi';

type Props = {
  params: Promise<{
    id: string;
  }>;
};

export default async function MatchFormatConfigPage({ params }: Props) {
  const { id } = await params;
  const pairs = await fetchPairs(id);
  return <RoundFormatTab tournamentId={id} pairs={pairs} />;
}
