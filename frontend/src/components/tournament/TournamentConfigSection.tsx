import { Settings, Users, Trophy, Zap } from 'lucide-react';
import { TournamentFormData } from '@/src/types/tournamentData';

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
              <option value="MIXED">Mixte</option>
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
              <option value="">Sélectionnez un format</option>
              <option value="KNOCKOUT">Élimination directe</option>
              <option value="QUALIF_KNOCKOUT">Qualif + Élimination directe</option>
              <option value="GROUP_STAGE">Phase de poules + Elim</option>
            </select>
          </div>
        </div>

        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          <div className="space-y-2 max-w-xs">
            <label htmlFor="nbSeeds" className="block text-sm font-medium text-foreground">
              Nombre de têtes de série
            </label>
            <input
              id="nbSeeds"
              name="nbSeeds"
              type="number"
              min="0"
              placeholder="0"
              value={formData.nbSeeds ?? ''}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            />
          </div>
          <div className="space-y-2 max-w-xs">
            <label htmlFor="nbMaxPairs" className="block text-sm font-medium text-foreground">
              Nombre d'équipes maximum
            </label>
            <input
              id="nbMaxPairs"
              name="nbMaxPairs"
              type="number"
              min="0"
              placeholder="Ex : 32"
              value={formData.nbMaxPairs ?? ''}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            />
          </div>
        </div>
      </div>
    </div>
  );
}