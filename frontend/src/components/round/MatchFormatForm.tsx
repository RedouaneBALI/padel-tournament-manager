'use client';

import React from 'react';
import { Switch } from '@/src/components/ui/switch';
import { MatchFormat }  from '@/src/types/matchFormat';

interface MatchFormatFormProps {
  format: MatchFormat;
  onChange: (newFormat: MatchFormat) => void;
}

export default function MatchFormatForm({ format, onChange }: MatchFormatFormProps) {
  type MatchFormatValue = number | boolean;
  const handleChange = (key: keyof MatchFormat, value: MatchFormatValue) => {
    onChange({ ...format, [key]: value });
  };

  return (
    <fieldset className="space-y-4 p-4 border rounded-md max-w-[600px] mx-auto">
      <legend className="font-semibold text-base mb-2 text-foreground">Format du match</legend>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* Sets à gagner */}
        <label className="flex flex-col text-sm">
          Sets à gagner
          <select
            value={format.numberOfSetsToWin}
            onChange={(e) => handleChange('numberOfSetsToWin', parseInt(e.target.value))}
            className="mt-1 rounded border border-input bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          >
            <option value={1}>1 set gagnant</option>
            <option value={2}>2 sets gagnants </option>
          </select>
        </label>

        {/* Points par set */}
        <label className="flex flex-col text-sm">
          Points par set
          <select
            value={format.pointsPerSet}
            onChange={(e) => handleChange('pointsPerSet', parseInt(e.target.value))}
            className="mt-1 rounded border border-input bg-background px-3 py-2 text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
          >
            <option value={4}>4 jeux</option>
            <option value={6}>6 jeux</option>
            <option value={9}>9 jeux</option>
            <option value={10}>10 jeux</option>
            <option value={15}>15 jeux</option>
          </select>
        </label>

        {/* Avantage */}
        <div className="flex items-center justify-between mt-2">
          <span className="text-sm">Avantages</span>
          <Switch
            checked={format.advantage}
            onCheckedChange={(value) => handleChange('advantage', value)}
          />
        </div>

        {/* Super tie-break au 3e set */}
        <div className="flex items-center justify-between mt-2">
          <span className="text-sm">3ᵉ set en super tie-break</span>
          <Switch
            checked={format.superTieBreakInFinalSet}
            onCheckedChange={(value) => handleChange('superTieBreakInFinalSet', value)}
          />
        </div>
      </div>
    </fieldset>
  );
}