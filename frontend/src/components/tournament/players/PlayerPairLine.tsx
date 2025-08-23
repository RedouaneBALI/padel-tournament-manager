'use client';

import { useEffect, useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { updatePlayerPair as apiUpdatePlayerPair } from '@/src/api/tournamentApi';
import SaveAndCancelButtons from '@/src/components/ui/SaveAndCancelButtons';
import { Edit3 } from 'lucide-react';

interface Props {
  pair: PlayerPair;
  tournamentId: string | number;
  editable?: boolean;
  isEditing: boolean;
  canEdit: boolean;
  onStartEdit: () => void;
  onCancelEdit: () => void;
  onSaved: (update: { player1Name?: string; player2Name?: string; seed?: number }) => void;
}

export default function PlayerPairLine({
  pair,
  tournamentId,
  editable = false,
  isEditing,
  canEdit,
  onStartEdit,
  onCancelEdit,
  onSaved,
}: Props) {
  const [p1, setP1] = useState<string>(pair.player1Name);
  const [p2, setP2] = useState<string>(pair.player2Name);
  const [seedStr, setSeedStr] = useState<string>(pair.seed?.toString() ?? '');
  const [isSaving, setIsSaving] = useState(false);

  // Reset inputs when the source pair or editing mode changes
  useEffect(() => {
    setP1(pair.player1Name);
    setP2(pair.player2Name);
    setSeedStr(pair.seed?.toString() ?? '');
  }, [pair.player1Name, pair.player2Name, pair.seed, isEditing]);

  const save = async () => {
    const id = pair.id ?? -1;
    if (id < 0) return;

    const payload: { player1Name?: string; player2Name?: string; seed?: number } = {};

    const p1Trim = p1.trim();
    const p2Trim = p2.trim();
    if (p1Trim && p1Trim !== pair.player1Name) payload.player1Name = p1Trim;
    if (p2Trim && p2Trim !== pair.player2Name) payload.player2Name = p2Trim;

    const parsedSeed = seedStr === '' ? undefined : Number(seedStr);
    if (Number.isFinite(parsedSeed as number) && parsedSeed !== pair.seed) payload.seed = parsedSeed as number;

    if (!payload.player1Name && !payload.player2Name && payload.seed === undefined) {
      onCancelEdit();
      return;
    }

    try {
      setIsSaving(true);
      await apiUpdatePlayerPair(tournamentId, id, payload);
      onSaved(payload); // parent applies the diff to its local snapshot
    } catch (e) {
      console.error(e);
    } finally {
      setIsSaving(false);
    }
  };

  if (!isEditing) {
    return (
      <div className="w-full flex items-center justify-between gap-3">
        <div className="flex-1 min-w-0 truncate">
          <span className="font-semibold text-primary">
            {pair.seed && pair.seed > 0 ? `#${pair.seed} ` : ''}
          </span>
          {pair.player1Name} – {pair.player2Name}
        </div>
        {editable && (
          <button
            type="button"
            className="inline-flex items-center justify-center rounded-md h-8 w-8 border border-input bg-background hover:bg-accent hover:text-accent-foreground disabled:opacity-50"
            aria-label="Modifier"
            title="Modifier"
            onClick={onStartEdit}
            disabled={!canEdit}
          >
            <Edit3 className="h-4 w-4" />
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="w-full flex items-center justify-between gap-3">
      <div className="flex-1 min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <input
            type="number"
            inputMode="numeric"
            className="w-16 border border-input bg-background rounded px-2 py-1"
            value={seedStr}
            onChange={(e) => setSeedStr(e.target.value)}
            placeholder="#"
            aria-label="Seed"
          />
          <input
            type="text"
            className="flex-1 min-w-[120px] border border-input bg-background rounded px-2 py-1"
            value={p1}
            onChange={(e) => setP1(e.target.value)}
            placeholder="Joueur 1"
            aria-label="Nom joueur 1"
          />
          <span className="text-muted">/</span>
          <input
            type="text"
            className="flex-1 min-w-[120px] border border-input bg-background rounded px-2 py-1"
            value={p2}
            onChange={(e) => setP2(e.target.value)}
            placeholder="Joueur 2"
            aria-label="Nom joueur 2"
          />
        </div>
      </div>
      <SaveAndCancelButtons
        isSaving={isSaving}
        onSave={save}
        bindEnter={editable}
        onCancel={() => {
          // reset inputs, then cancel
          setP1(pair.player1Name);
          setP2(pair.player2Name);
          setSeedStr(pair.seed?.toString() ?? '');
          onCancelEdit();
        }}
      />
    </div>
  );
}
