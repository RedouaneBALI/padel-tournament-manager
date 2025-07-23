import { MapPin, Building, FileText } from 'lucide-react';

interface TournamentInfoSectionProps {
  formData: any;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
}

export default function TournamentInfoSection({ formData, handleInputChange }: TournamentInfoSectionProps) {
  return (
    <div className="bg-card">
      <div className="p-4 border-b border-border">
        <h3 className="text-lg font-semibold text-card-foreground flex items-center gap-2">
          <FileText className="h-5 w-5" />
          Informations principales
        </h3>
      </div>
      <div className="p-4 space-y-6">
        <div className="grid gap-6 md:grid-cols-2">
          <div className="space-y-2">
            <label htmlFor="name" className="block text-sm font-medium text-foreground">
              Nom du tournoi *
            </label>
            <input
              id="name"
              name="name"
              type="text"
              placeholder="Ex: Open de Padel 2025"
              value={formData.name}
              onChange={handleInputChange}
              required
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            />
          </div>
          
          <div className="space-y-2">
            <label htmlFor="club" className="block text-sm font-medium text-foreground flex items-center gap-2">
              <Building className="h-4 w-4" />
              Club organisateur
            </label>
            <input
              id="club"
              name="club"
              type="text"
              placeholder="Nom du club"
              value={formData.club}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            />
          </div>
          
          <div className="space-y-2">
            <label htmlFor="city" className="block text-sm font-medium text-foreground flex items-center gap-2">
              <MapPin className="h-4 w-4" />
              Ville
            </label>
            <input
              id="city"
              name="city"
              type="text"
              placeholder="Ville du tournoi"
              value={formData.city}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            />
          </div>
        </div>
        
        <div className="space-y-2">
          <label htmlFor="description" className="block text-sm font-medium text-foreground">
            Description
          </label>
          <textarea
            id="description"
            name="description"
            placeholder="Décrivez votre tournoi..."
            value={formData.description}
            onChange={handleInputChange}
            rows={3}
            className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors min-h-[100px] resize-none"
          />
        </div>
      </div>
    </div>
  );
}