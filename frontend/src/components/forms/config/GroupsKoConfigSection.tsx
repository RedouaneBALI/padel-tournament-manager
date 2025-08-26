import { useEffect } from 'react';
import type { TournamentFormData } from '@/src/validation/tournament';
import { Grid, Users, Hash, CheckSquare } from 'lucide-react';
import { TbTournament } from "react-icons/tb";

interface GroupsKoConfigSectionProps {
  formData: TournamentFormData;
  handleInputChange: (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
}

const toStr = (v: unknown) => (v == null ? '' : String(v));

export default function GroupsKoConfigSection({
  formData,
  handleInputChange
}: GroupsKoConfigSectionProps) {
  const cfg = formData.config as any;

  useEffect(() => {
    handleInputChange({
      target: {
        name: 'config.nbSeeds',
        value: String(cfg.nbPools ?? 4)
      }
    } as React.ChangeEvent<HTMLInputElement>);
  }, []);

  return (
    <>
      <div className="space-y-2">
        <label htmlFor="nbPools" className="flex items-center gap-2 text-sm font-medium text-foreground leading-none">
          <Grid className="h-4 w-4" />
          Nombre de poules
        </label>
        <select
          id="nbPools"
          name="config.nbPools"
          value={toStr(cfg.nbPools ?? 4)}
          onChange={handleInputChange}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
        >
          <option value={2}>2</option>
          <option value={4}>4</option>
          <option value={8}>8</option>
        </select>
      </div>
      <div className="space-y-2">
        <label htmlFor="nbPairsPerPool" className="flex items-center gap-2 text-sm font-medium text-foreground leading-none">
          <Users className="h-4 w-4" />
          Équipes par poule
        </label>
        <input
          id="nbPairsPerPool"
          name="config.nbPairsPerPool"
          type="number"
          min="3"
          max="8"
          value={
            cfg.nbPairsPerPool === undefined || Number.isNaN(cfg.nbPairsPerPool)
              ? ''
              : String(cfg.nbPairsPerPool)
          }
          onChange={handleInputChange}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
        />
      </div>
      <div className="space-y-2">
        <label htmlFor="nbSeeds" className="flex items-center gap-2 text-sm font-medium text-foreground leading-none">
          <Hash className="h-4 w-4" />
          Nombre de têtes de série (0 si tirage aléatoire)
        </label>
        <select
          id="nbSeeds"
          name="config.nbSeeds"
          value={toStr(cfg.nbSeeds ?? cfg.nbPools ?? 4)}
          onChange={handleInputChange}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
        >
          {[0, 2, 4, 8, 16].map((val) => (
            <option key={val} value={val}>{val}</option>
          ))}
        </select>
      </div>
      <div className="space-y-2">
        <label htmlFor="nbQualifiedByPool" className="flex items-center gap-2 text-sm font-medium text-foreground leading-none">
          <CheckSquare className="h-4 w-4" />
          Équipes qualifiées par poule
        </label>
        {(() => {
          const nbPairsPerPool = Number(cfg.nbPairsPerPool ?? 4);
          const qualifiedOptions = Array.from({ length: nbPairsPerPool }, (_, i) => i + 1).filter(
            (n) => (n & (n - 1)) === 0
          );

          return (
            <select
              id="nbQualifiedByPool"
              name="config.nbQualifiedByPool"
              value={toStr(cfg.nbQualifiedByPool ?? 2)}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
            >
              {qualifiedOptions.map((val) => (
                <option key={val} value={val}>{val}</option>
              ))}
            </select>
          );
        })()}
      </div>
    </>
  );
}