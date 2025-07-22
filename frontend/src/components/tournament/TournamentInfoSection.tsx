export default function TournamentInfoSection({ formData, handleInputChange }) {
  return (
    <section className="space-y-6">
      <h3 className="text-lg font-semibold text-gray-900">Informations principales</h3>
      <div className="grid gap-6 md:grid-cols-2">
        <div className="space-y-2">
          <label htmlFor="name" className="block text-sm font-medium text-gray-700">Nom du tournoi *</label>
          <input
            id="name"
            name="name"
            type="text"
            placeholder="Ex: Open de Padel 2025"
            value={formData.name}
            onChange={handleInputChange}
            required
            className="w-full px-3 py-2 border border-gray-300 rounded-md"
          />
        </div>
        <div className="space-y-2">
          <label htmlFor="club" className="block text-sm font-medium text-gray-700">Club organisateur</label>
          <input
            id="club"
            name="club"
            type="text"
            placeholder="Nom du club"
            value={formData.club}
            onChange={handleInputChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-md"
          />
        </div>
        <div className="space-y-2">
          <label htmlFor="city" className="block text-sm font-medium text-gray-700">Ville</label>
          <input
            id="city"
            name="city"
            type="text"
            placeholder="Ville du tournoi"
            value={formData.city}
            onChange={handleInputChange}
            className="w-full px-3 py-2 border border-gray-300 rounded-md"
          />
        </div>
      </div>
      <div className="space-y-2">
        <label htmlFor="description" className="block text-sm font-medium text-gray-700">Description</label>
        <textarea
          id="description"
          name="description"
          placeholder="Décrivez votre tournoi..."
          value={formData.description}
          onChange={handleInputChange}
          rows={3}
          className="w-full px-3 py-2 border border-gray-300 rounded-md"
        />
      </div>
    </section>
  );
}
