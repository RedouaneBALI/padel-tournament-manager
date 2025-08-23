// src/components/round/MatchFormatForm.tsx
'use client';

import React from 'react';
import { Switch } from '@/src/components/ui/Switch';
import { MatchFormat } from '@/src/types/matchFormat';

interface MatchFormatFormProps {
  format: MatchFormat;
  onChange?: (newFormat: MatchFormat) => void;
  readOnly?: boolean;
}

export default function MatchFormatForm({
  format,
  onChange,
  readOnly = false,
}: MatchFormatFormProps) {
  type MatchFormatValue = number | boolean;

  const handleChange = (key: keyof MatchFormat, value: MatchFormatValue) => {
    if (!onChange) return;
    onChange({ ...format, [key]: value });
  };

  return (
    <fieldset className="space-y-4 p-4 border rounded-md max-w-[600px] mx-auto">
      <legend className="font-semibold text-base mb-2 text-foreground">
        Format du match
      </legend>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* Sets à gagner */}
        <label className="flex flex-col text-sm">
          Sets à gagner
          {readOnly ? (
            <select
              value={format.numberOfSetsToWin}
              disabled
              aria-readonly
              className="mt-1 rounded border border-input bg-background px-3 py-2 text-foreground pointer-events-none opacity-60"
            >
              <option value={1}>1 set gagnant</option>
              <option value={2}>2 sets gagnants</option>
            </select>
          ) : (
            <select
              value={format.numberOfSetsToWin}
              onChange={(e) => handleChange('numberOfSetsToWin', parseInt(e.target.value))}
              className="mt-1 rounded border border-input bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
            >
              <option value={1}>1 set gagnant</option>
              <option value={2}>2 sets gagnants</option>
            </select>
          )}
        </label>

        {/* Jeux par set */}
        <label className="flex flex-col text-sm">
          Jeux par set
          {readOnly ? (
            <select
              value={format.gamesPerSet}
              disabled
              aria-readonly
              className="mt-1 rounded border border-input bg-background px-3 py-2 text-foreground pointer-events-none opacity-60"
            >
              <option value={4}>4 jeux</option>
              <option value={6}>6 jeux</option>
              <option value={9}>9 jeux</option>
              <option value={10}>10 jeux</option>
              <option value={15}>15 jeux</option>
            </select>
          ) : (
            <select
              value={format.gamesPerSet}
              onChange={(e) => handleChange('gamesPerSet', parseInt(e.target.value))}
              className="mt-1 rounded border border-input bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
            >
              <option value={4}>4 jeux</option>
              <option value={6}>6 jeux</option>
              <option value={9}>9 jeux</option>
              <option value={10}>10 jeux</option>
              <option value={15}>15 jeux</option>
            </select>
          )}
        </label>

        {/* Avantages */}
        <div className="flex items-center justify-between mt-2">
          <span className="text-sm">Avantages</span>
          {readOnly ? (
            <Switch
              checked={!!format.advantage}
              disabled
              aria-readonly
              className="pointer-events-none opacity-60"
            />
          ) : (
            <Switch
              checked={format.advantage}
              onCheckedChange={(value) => handleChange('advantage', value)}
            />
          )}
        </div>

        {/* Super tie-break au 3e set */}
        <div className="flex items-center justify-between mt-2">
          <span className="text-sm">3ᵉ set en super tie-break</span>
          {readOnly ? (
            <Switch
              checked={!!format.superTieBreakInFinalSet}
              disabled
              aria-readonly
              className="pointer-events-none opacity-60"
            />
          ) : (
            <Switch
              checked={format.superTieBreakInFinalSet}
              onCheckedChange={(value) => handleChange('superTieBreakInFinalSet', value)}
            />
          )}
        </div>
      </div>
    </fieldset>
  );
}