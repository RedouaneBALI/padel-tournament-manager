export default function TournamentConfigSection({ formData, handleInputChange, handleSelectChange }) {
  return (
    <section className="space-y-6">
      <h3 className="text-lg font-semibold text-gray-900">Configuration</h3>
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        <select name="gender" value={formData.gender} onChange={handleSelectChange} className="w-full px-3 py-2 border rounded-md">
          <option value="">Sélectionnez un genre</option>
          <option value="MALE">Homme</option>
          <option value="FEMALE">Femme</option>
          <option value="MIXED">Mixte</option>
        </select>

        <select name="level" value={formData.level} onChange={handleSelectChange} className="w-full px-3 py-2 border rounded-md">
          <option value="">Sélectionnez un niveau</option>
          <option value="REGIONAL">Régional</option>
          <option value="NATIONAL">National</option>
          <option value="INTERNATIONAL">International</option>
        </select>

        <select name="tournamentFormat" value={formData.tournamentFormat} onChange={handleSelectChange} className="w-full px-3 py-2 border rounded-md">
          <option value="">Sélectionnez un format</option>
          <option value="KNOCKOUT">Élimination directe</option>
          <option value="GROUP_STAGE">Phase de poules</option>
        </select>
      </div>
      <input
        name="nbSeeds"
        type="number"
        min="0"
        placeholder="Nombre de têtes de série"
        value={formData.nbSeeds}
        onChange={handleInputChange}
        className="w-full md:max-w-xs px-3 py-2 border rounded-md"
      />
    </section>
  );
}
