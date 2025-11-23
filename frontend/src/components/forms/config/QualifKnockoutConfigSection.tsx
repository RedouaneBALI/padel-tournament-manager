// src/components/forms/config/QualifKnockoutConfigSection.tsx
import type { TournamentFormData } from '@/src/validation/tournament';
import KnockoutConfigSection from '@/src/components/forms/config/KnockoutConfigSection';
import { Hash, Trophy } from 'lucide-react';
import { TbTournament } from "react-icons/tb";

interface QualifKnockoutConfigSectionProps {
  formData: TournamentFormData;
  handleInputChange: (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  isTournamentStarted?: boolean;
}

const toStr = (v: unknown) => (v == null ? '' : String(v));

export default function QualifKnockoutConfigSection({
  formData,
  handleInputChange,
  isTournamentStarted = false,
}: QualifKnockoutConfigSectionProps) {
  const cfg = formData.config as any;
  const preQualSize = Number(cfg.preQualDrawSize ?? 16);
  const maxSeeds = preQualSize / 2;
  const seedOptions = [];
  for (let i = 0; i <= maxSeeds; i += 2) {
    seedOptions.push(i);
  }

  return (
    <>
      {/* Taille du tableau de pré-qualif (puissance de 2) */}
      <div className="space-y-2">
        <label htmlFor="preQualDrawSize" className="flex items-center gap-2 text-sm font-medium text-foreground leading-none">
          <TbTournament className="h-4 w-4" />
          Taille du tableau de pré-qualif (paires)
        </label>
        <select
          id="preQualDrawSize"
          name="config.preQualDrawSize"
          value={toStr(cfg.preQualDrawSize ?? 16)}
          onChange={handleInputChange}
          disabled={isTournamentStarted}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10 disabled:opacity-50 disabled:cursor-not-allowed"
          title={isTournamentStarted ? "Impossible de modifier une fois le tournoi lancé" : ""}
        >
          <option value={8}>8</option>
          <option value={16}>16</option>
          <option value={32}>32</option>
        </select>
      </div>
      <div className="space-y-2">
        <label htmlFor="nbSeedsQualify" className="flex items-center gap-2 text-sm font-medium text-foreground leading-none">
          <Hash className="h-4 w-4" />
          Têtes de série en qualif (0 si tirage aléatoire)
        </label>
        <select
          id="nbSeedsQualify"
          name="config.nbSeedsQualify"
          value={cfg.nbSeedsQualify ?? Math.floor(preQualSize / 4)}
          onChange={handleInputChange}
          disabled={isTournamentStarted}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10 disabled:opacity-50 disabled:cursor-not-allowed"
          title={isTournamentStarted ? "Impossible de modifier une fois le tournoi lancé" : ""}
        >
          {seedOptions.map((val) => (
            <option key={val} value={val}>{val}</option>
          ))}
        </select>
      </div>

      {/* Nombre de qualifiés depuis les pré-qualifs */}
      <div className="space-y-2">
        <label htmlFor="nbQualifiers" className="flex items-center gap-2 text-sm font-medium text-foreground leading-none">
          <Trophy className="h-4 w-4" />
          Nombre de qualifiés
        </label>
        <select
          id="nbQualifiers"
          name="config.nbQualifiers"
          value={toStr(cfg.nbQualifiers ?? 4)}
          onChange={handleInputChange}
          disabled={isTournamentStarted}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10 disabled:opacity-50 disabled:cursor-not-allowed"
          title={isTournamentStarted ? "Impossible de modifier une fois le tournoi lancé" : ""}
        >
          <option value={2}>2</option>
          <option value={4}>4</option>
          <option value={8}>8</option>
        </select>
      </div>

      <KnockoutConfigSection
        formData={formData}
        handleInputChange={handleInputChange}
        isTournamentStarted={isTournamentStarted}
      />
    </>
  );
}

