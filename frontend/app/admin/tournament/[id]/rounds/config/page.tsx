'use client';

import React from 'react';
import { use } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import RoundFormatTab from '@/src/components/round/RoundFormatTab';

interface PageProps {
  params: Promise<{ id: string }>;
  pairs: PlayerPair[];
}

export default function MatchFormatConfigPage({ params, pairs }: PageProps) {
  const { id } = use(params);
  return <RoundFormatTab tournamentId={id} pairs={pairs} />;
}