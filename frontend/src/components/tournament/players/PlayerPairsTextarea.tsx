'use client';

import { useEffect, useState } from 'react';
import { PlayerPair } from '@/src/types/playerPair';
import { toast } from 'react-toastify';
import { fetchPairs, savePlayerPairs } from '@/src/api/tournamentApi';
import CenteredLoader from '@/src/components/ui/CenteredLoader';

interface Props {
  tournamentId: string;
  onPairsChange?: (pairs: PlayerPair[]) => void | Promise<void>; // OK dans interface
  hasStarted?: boolean;
  onSaveSuccess?: () => void;
}

export default function PlayerPairsTextarea({ tournamentId, onPairsChange, hasStarted, onSaveSuccess }: Props) {
  const [text, setText] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const fetchUpdatedPairs = async () => {
    setIsLoading(true);
    try {
      const data = await fetchPairs(tournamentId, false, false);
      setText(data.map(pair => `${pair.player1Name},${pair.player2Name}${pair.displaySeed ? ',' + pair.displaySeed : ''}`).join('\n'));
      if (onPairsChange) onPairsChange(data);
    } catch (e: any) {
      toast.error(e?.message ?? 'Impossible de charger les paires.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchUpdatedPairs();
  }, [tournamentId]);

  const handleClear = () => setText('');

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const lines = text
        .split('\n')
        .map(line => line.trim())
        .filter(line => line !== '');

      const pairs: PlayerPair[] = lines.map((line, index) => {
        // Split by common separators, but only take up to 3 tokens: p1, p2, seed
        const tokens = line.split(/[,;&/]/, 3).map(s => s.trim());
        const p1 = tokens[0] ?? '';
        const p2 = tokens[1] ?? '';
        const seedRaw = tokens[2];

        // Only use seed if a 3rd token exists and is a valid number
        const parsedSeed = seedRaw !== undefined && seedRaw !== '' ? Number(seedRaw) : NaN;
        const hasSeed = Number.isFinite(parsedSeed);

        const pair: PlayerPair = {
          player1Name: p1,
          player2Name: p2,
          type: 'NORMAL' as const,
        };

        // Only include seed and displaySeed if explicitly specified
        if (hasSeed) {
          pair.seed = parsedSeed as number;
          pair.displaySeed = parsedSeed.toString();
        }

        return pair;
      });
      await savePlayerPairs(tournamentId, pairs);
      if (onSaveSuccess) onSaveSuccess();
    } catch {
      return;
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <>
      {isSaving && (
        <CenteredLoader />
      )}
      <div className="bg-card">
        {isLoading ? (
        <CenteredLoader />
        ) : (
          <>
            <textarea
              className="w-full h-60 p-2 border border-border rounded resize-none font-mono overflow-x-auto whitespace-pre"
              wrap="off"
              value={text}
              onChange={e => setText(e.target.value)}
              placeholder={`Ghali Berrada,Selim Mekouar,1\nRedouane Bali,Ali Khobzaoui,2`}
              disabled={hasStarted}
            />

            {!hasStarted && (
              <div className="flex justify-center gap-4 py-2">
                <button
                  onClick={handleClear}
                  className="px-4 py-2 border border-border text-foreground rounded hover:bg-background"
                >
                  Effacer
                </button>
                <button
                  onClick={handleSave}
                  className="px-4 py-2 bg-primary text-on-primary rounded hover:bg-primary-hover"
                >
                  Enregistrer les joueurs
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </>
  );
}