import { Settings, Users, Trophy, Zap } from 'lucide-react';
import type { TournamentFormData } from '@/src/validation/tournament';

interface TournamentConfigSectionProps {
  formData: TournamentFormData;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
}

export default function TournamentConfigSection({ 
  formData, 
  handleInputChange
}: TournamentConfigSectionProps) {
  return (
    <div className="bg-card">
      <div className="p-4 border-b border-border">
        <h3 className="text-lg font-semibold text-card-foreground flex items-center gap-2">
          <Settings className="h-5 w-5" />
          Configuration
        </h3>
      </div>
      <div className="p-4 space-y-6">
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <div className="space-y-2">
            <label className="block text-sm font-medium text-foreground flex items-center gap-2">
              <Users className="h-4 w-4" />
              Genre
            </label>
            <select
              name="gender"
              value={formData.gender ?? ''}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            >
              <option value="">Sélectionnez un genre</option>
              <option value="MEN">Homme</option>
              <option value="WOMEN">Femme</option>
              <option value="MIX">Mixte</option>
            </select>
          </div>

          <div className="space-y-2">
            <label className="block text-sm font-medium text-foreground flex items-center gap-2">
              <Trophy className="h-4 w-4" />
              Niveau
            </label>
            <select
              name="level"
              value={formData.level ?? ''}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            >
              <option value="">Sélectionnez un niveau</option>
              <option value="AMATEUR">Amateur</option>
              <option value="P25">P25</option>
              <option value="P50">P50</option>
              <option value="P100">P100</option>
              <option value="P250">P250</option>
              <option value="P500">P500</option>
              <option value="P1000">P1000</option>
              <option value="P2000">P2000</option>
            </select>
          </div>

          <div className="space-y-2">
            <label className="block text-sm font-medium text-foreground flex items-center gap-2">
              <Zap className="h-4 w-4" />
              Format
            </label>
        <select
          name="tournamentFormat"
          value={formData.tournamentFormat ?? ''}
          onChange={handleInputChange}
          className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
        >
          <option value="KNOCKOUT">Élimination directe</option>
          <option value="GROUP_STAGE"> Poules + Elimination directe </option>
          <option value="QUALIF_KNOCKOUT" disabled className="text-muted-foreground cursor-not-allowed">
            Qualif + Élimination directe
          </option>
        </select>
          </div>
        </div>

        <div className="grid gap-6 grid-cols-1 md:grid-cols-2 md:max-w-3xl">
          <div className="space-y-2">
            <label htmlFor="nbSeeds" className="block text-sm font-medium text-foreground">
              Nombre de têtes de série (0 si tirage aléatoire)
            </label>
            <input
              id="nbSeeds"
              name="nbSeeds"
              type="number"
              min="0"
              max="64"
              value={formData.nbSeeds ?? 16}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            />
          </div>
          {formData.tournamentFormat !== 'GROUP_STAGE' && (
            <div className="space-y-2">
              <label htmlFor="nbMaxPairs" className="block text-sm font-medium text-foreground">
                Nombre d&apos;équipes maximum
              </label>
              <input
                id="nbMaxPairs"
                name="nbMaxPairs"
                type="number"
                min="0"
                max="128"
                value={formData.nbMaxPairs ?? 48}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
              />
            </div>
          )}
          {formData.tournamentFormat === 'GROUP_STAGE' && (
            <>
              <div className="space-y-2">
                <label htmlFor="nbPools" className="block text-sm font-medium text-foreground">
                  Nombre de poules
                </label>
                <select
                  id="nbPools"
                  name="nbPools"
                  value={formData.nbPools ?? 4}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
                >
                  <option value={2}>2</option>
                  <option value={4}>4</option>
                  <option value={8}>8</option>
                </select>
              </div>

              <div className="space-y-2">
                <label htmlFor="nbPairsPerPool" className="block text-sm font-medium text-foreground">
                  Équipes par poule
                </label>
                <input
                  id="nbPairsPerPool"
                  name="nbPairsPerPool"
                  type="number"
                  min="3"
                  max="8"
                  value={formData.nbPairsPerPool ?? 4}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="nbQualifiedByPool" className="block text-sm font-medium text-foreground">
                  Équipes qualifiées par poule
                </label>
                <select
                  id="nbQualifiedByPool"
                  name="nbQualifiedByPool"
                  value={formData.nbQualifiedByPool ?? 2}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors h-10"
                >
                  <option value={1}>1</option>
                  <option value={2}>2</option>
                  <option value={4}>4</option>
                </select>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}