'use client';

import { Settings, Users, Trophy, Zap } from 'lucide-react';
import type { TournamentFormData } from '@/src/validation/tournament';
import KnockoutConfigSection from '@/src/components/forms/config/KnockoutConfigSection';
import GroupsKoConfigSection from '@/src/components/forms/config/GroupsKoConfigSection';
import QualifKnockoutConfigSection from '@/src/components/forms/config/QualifKnockoutConfigSection';
import { ReactMultiEmail } from 'react-multi-email';
import 'react-multi-email/dist/style.css';

const toStr = (v: unknown) => (v == null ? '' : String(v));

interface TournamentConfigSectionProps {
  formData: TournamentFormData;
  handleInputChange: (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => void;
  onEmailsChange: (emails: string[]) => void;
}

export default function TournamentConfigSection({
  formData,
  handleInputChange,
  onEmailsChange
}: TournamentConfigSectionProps) {
  const emails = Array.isArray(formData.editorIds) ? formData.editorIds : [];

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
              value={toStr(formData.gender)}
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
              value={toStr(formData.level)}
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
              name="config.format"
              value={toStr(formData.config?.format ?? '')}
              onChange={handleInputChange}
              required
              className="w-full px-3 py-2 border border-input rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-colors"
            >
              <option value="KNOCKOUT">Élimination directe</option>
              <option value="QUALIF_KO"> Qualif + Élimination directe </option>
              <option value="GROUPS_KO" disabled> Poules + Elimination directe </option>
            </select>
          </div>


        </div>

        <div className="grid gap-6 grid-cols-1 md:grid-cols-2 md:max-w-3xl">
          {formData.config?.format === 'KNOCKOUT' && (
            <KnockoutConfigSection formData={formData} handleInputChange={handleInputChange} />
          )}
          {formData.config?.format === 'GROUPS_KO' && (
            <GroupsKoConfigSection formData={formData} handleInputChange={handleInputChange} />
          )}
          {formData.config?.format === 'QUALIF_KO' && (
            <QualifKnockoutConfigSection formData={formData} handleInputChange={handleInputChange} />
          )}
        </div>

        {/* Editors emails */}
        <div className="pt-4 border-t border-border">
          <label className="block text-sm font-medium text-foreground mb-2">Éditeurs (emails)</label>
          <div className="[&_.react-multi-email]:min-h-[80px] [&_.react-multi-email]:border [&_.react-multi-email]:border-input [&_.react-multi-email]:rounded-md [&_.react-multi-email]:p-2 [&_.react-multi-email]:bg-background [&_.react-multi-email]:text-foreground">
            <ReactMultiEmail
              emails={emails}
              onChange={onEmailsChange}
              placeholder="Entrez les adresses email des éditeurs"
              getLabel={(email, index, removeEmail) => {
                return (
                  <div data-tag key={index} className="inline-flex items-center gap-1 px-2 py-1 mr-1 mb-1 rounded bg-primary/10 text-primary text-sm">
                    <span>{email}</span>
                    <button
                      type="button"
                      onClick={() => removeEmail(index)}
                      className="ml-1 text-lg leading-none hover:opacity-70"
                    >
                      ×
                    </button>
                  </div>
                );
              }}
            />
          </div>
          <p className="text-xs text-muted-foreground mt-2">
            Appuyez sur Entrée, Tab ou Espace pour ajouter une adresse.
          </p>
        </div>

      </div>
    </div>
  );
}