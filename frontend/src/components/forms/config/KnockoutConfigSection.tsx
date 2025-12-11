import React, { useEffect, useRef } from 'react';
import { Hash } from 'lucide-react';
import { TbTournament } from "react-icons/tb";
import type { TournamentFormData } from '@/src/validation/tournament';

interface KnockoutConfigSectionProps {
  formData: TournamentFormData;
  handleInputChange: (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  isTournamentStarted?: boolean;
}

export default function KnockoutConfigSection({
  formData,
  handleInputChange,
  isTournamentStarted = false
}: KnockoutConfigSectionProps) {
  // S'assurer que format existe avec des valeurs par défaut
  const cfg = formData.config || { mainDrawSize: 32, nbSeeds: 16 };

  const mainDrawSize = Number((cfg as any).mainDrawSize ?? 32);
  const defaultNbSeeds = mainDrawSize / 4;
  const seedOptions = [0];
  for (let i = 2; i <= defaultNbSeeds*2; i *= 2) {
    seedOptions.push(i);
  }

  const hasChangedNbSeeds = useRef(false);

  useEffect(() => {
    if (!hasChangedNbSeeds.current) {
      const syntheticEvent = {
        target: {
          name: 'config.nbSeeds',
          value: defaultNbSeeds.toString(),
        },
      } as unknown as React.ChangeEvent<HTMLSelectElement>;
      handleInputChange(syntheticEvent);
    }
  }, [mainDrawSize]);

  return (
    <>
      <div className="space-y-2">
        <label htmlFor="mainDrawSize" className="flex items-center gap-2 text-sm font-medium text-foreground leading-none">
          <TbTournament className="h-4 w-4" />
          Taille du tableau principal (paires)
        </label>
        <select
          id="mainDrawSize"
          name="config.mainDrawSize"
          value={Number((cfg as any).mainDrawSize ?? 32)}
          onChange={(e) => {
            handleInputChange(e);
          }}
          disabled={isTournamentStarted}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10 disabled:opacity-50 disabled:cursor-not-allowed"
          title={isTournamentStarted ? "Impossible de modifier la taille une fois le tournoi lancé" : ""}
        >
          {[2,4, 8, 16, 32, 64, 128].map((val) => (
            <option key={val} value={val}>{val}</option>
          ))}
        </select>
      </div>
      <div className="space-y-2">
        <label htmlFor="nbSeeds" className="flex items-center gap-2 text-sm font-medium text-foreground leading-none">
          <Hash className="h-4 w-4" />
          Têtes de série (0 si tirage aléatoire)
        </label>
        <select
          id="nbSeeds"
          name="config.nbSeeds"
          value={Number((cfg as any).nbSeeds ?? defaultNbSeeds)}
          onChange={(e) => {
            hasChangedNbSeeds.current = true;
            handleInputChange(e);
          }}
          disabled={isTournamentStarted}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10 disabled:opacity-50 disabled:cursor-not-allowed"
          title={isTournamentStarted ? "Impossible de modifier les têtes de série une fois le tournoi lancé" : ""}
        >
          {seedOptions.map((val) => (
            <option key={val} value={val}>{val}</option>
          ))}
        </select>
      </div>
    </>
  );
}

