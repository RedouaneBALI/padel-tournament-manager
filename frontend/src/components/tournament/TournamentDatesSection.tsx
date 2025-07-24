import { Calendar } from 'lucide-react';
import { TournamentFormData } from '@/src/types/tournamentData';

interface TournamentDatesSectionProps {
  formData: TournamentFormData;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

export default function TournamentDatesSection({ formData, handleInputChange }: TournamentDatesSectionProps) {
  return (
    <div className="bg-card">
      <div className="p-4 border-b border-border">
        <h3 className="text-lg font-semibold text-card-foreground flex items-center gap-2">
          <Calendar className="h-5 w-5" />
          Dates du tournoi
        </h3>
      </div>
      <div className="p-4">
        <div className="grid gap-6 md:grid-cols-2">
          <div className="space-y-2">
            <label htmlFor="startDate" className="block text-sm font-medium text-foreground">
              Date de début
            </label>
            <input
              id="startDate"
              name="startDate"
              type="date"
              value={formData.startDate ?? ''}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            />
          </div>
          
          <div className="space-y-2">
            <label htmlFor="endDate" className="block text-sm font-medium text-foreground">
              Date de fin
            </label>
            <input
              id="endDate"
              name="endDate"
              type="date"
              value={formData.endDate ?? ''}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            />
          </div>
        </div>
      </div>
    </div>
  );
}