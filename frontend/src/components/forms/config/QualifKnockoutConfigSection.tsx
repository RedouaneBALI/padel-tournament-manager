// src/components/forms/config/QualifKnockoutConfigSection.tsx
import type { TournamentFormData } from '@/src/validation/tournament';
import KnockoutConfigSection from '@/src/components/forms/config/KnockoutConfigSection';

interface QualifKnockoutConfigSectionProps {
  formData: TournamentFormData;
  handleInputChange: (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
}

const toStr = (v: unknown) => (v == null ? '' : String(v));

export default function QualifKnockoutConfigSection({
  formData,
  handleInputChange,
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
        <label htmlFor="preQualDrawSize" className="block text-sm font-medium text-foreground">
          Taille du tableau de pré-qualif (paires)
        </label>
        <select
          id="preQualDrawSize"
          name="config.preQualDrawSize"
          value={toStr(cfg.preQualDrawSize ?? 16)}
          onChange={handleInputChange}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
        >
          <option value={8}>8</option>
          <option value={16}>16</option>
          <option value={32}>32</option>
        </select>
      </div>
      <div className="space-y-2">
        <label htmlFor="nbSeedsQualify" className="block text-sm font-medium text-foreground">
          Nombre de tête de série en qualif (0 si tirage aléatoire)
        </label>
        <select
          id="nbSeedsQualify"
          name="config.nbSeedsQualify"
          value={cfg.nbSeedsQualify ?? Math.floor(preQualSize / 2)}
          onChange={handleInputChange}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
        >
          {seedOptions.map((val) => (
            <option key={val} value={val}>{val}</option>
          ))}
        </select>
      </div>

      {/* Nombre de qualifiés depuis les pré-qualifs */}
      <div className="space-y-2">
        <label htmlFor="nbQualifiers" className="block text-sm font-medium text-foreground">
          Nombre de qualifiés
        </label>
        <select
          id="nbQualifiers"
          name="config.nbQualifiers"
          value={toStr(cfg.nbQualifiers ?? 4)}
          onChange={handleInputChange}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
        >
          <option value={2}>2</option>
          <option value={4}>4</option>
          <option value={8}>8</option>
        </select>
      </div>

      <KnockoutConfigSection formData={formData} handleInputChange={handleInputChange} />
    </>
  );
}