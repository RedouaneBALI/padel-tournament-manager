'use client';

import { useEffect, useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import CenteredLoader from '@/src/components/ui/CenteredLoader';
import PlayerPairLine from './PlayerPairLine';

interface PlayerPairListProps {
  pairs: PlayerPair[];
  tournamentId: string | number;
  loading?: boolean;
  editable?: boolean;
}

export default function PlayerPairList({ pairs, tournamentId, loading = false, editable = false }: PlayerPairListProps) {
  const [editingPairId, setEditingPairId] = useState<number | null>(null);
  const [localPairs, setLocalPairs] = useState<PlayerPair[]>(pairs ?? []);

  // Keep local snapshot in sync when parent updates its list
  useEffect(() => {
    setLocalPairs(pairs ?? []);
  }, [pairs]);

  const hasPairs = (localPairs?.length ?? 0) > 0;
  if (!hasPairs) {
    if (loading) return <CenteredLoader />;
    return <p className="text-muted italic">Aucune paire inscrite pour le moment.</p>;
  }

  const startEdit = (pairId?: number) => {
    if (!editable || tournamentId === undefined || tournamentId === null) return;
    if (!pairId) return;
    setEditingPairId(pairId);
  };

  const cancelEdit = () => setEditingPairId(null);

  const applySavedChanges = (pairId: number, update: { player1Name?: string; player2Name?: string; seed?: number }) => {
    setLocalPairs(prev => prev.map(p => {
      if ((p.id ?? -1) !== pairId) return p;
      return {
        ...p,
        displaySeed: update.seed !== undefined ? update.seed.toString() : p.displaySeed,
        player1Name: update.player1Name !== undefined ? update.player1Name : p.player1Name,
        player2Name: update.player2Name !== undefined ? update.player2Name : p.player2Name,
      };
    }));
    setEditingPairId(null);
  };

  return (
    <>
      <div className="flex items-center">
        <div className="h-px flex-1 bg-border  my-6" />
        <h3 className="text-s sm:text-sm uppercase tracking-wider text-muted-foreground select-none">{pairs.length} Equipes inscrites</h3>
        <div className="h-px flex-1 bg-border" />
      </div>
      <ul className="space-y-2">
        {localPairs.map((pair) => {
          const id = pair.id ?? -1;
          const isEditing = editingPairId === id;
          const canEdit = editable && (editingPairId === null || isEditing);
          return (
              <li key={id} className="border border-primary/10 rounded px-4 py-2 bg-primary/5 shadow-sm text-sm flex items-center justify-between gap-3">
              <PlayerPairLine
                pair={pair}
                tournamentId={tournamentId}
                editable={editable}
                isEditing={isEditing}
                canEdit={canEdit}
                onStartEdit={() => startEdit(id)}
                onCancelEdit={cancelEdit}
                onSaved={(update) => applySavedChanges(id, update)}
              />
            </li>
          );
        })}
      </ul>
      </>
  );
}