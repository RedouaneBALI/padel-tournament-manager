export default function TournamentLocationSection({ formData, handleInputChange }) {
  return (
    <section className="space-y-6">
      <h3 className="text-lg font-semibold text-gray-900">Localisation</h3>
      <div className="space-y-2">
        <label htmlFor="city" className="block text-sm font-medium text-gray-700">Ville</label>
        <input
          id="city"
          name="city"
          type="text"
          placeholder="Ville du tournoi"
          value={formData.city}
          onChange={handleInputChange}
          className="w-full md:max-w-md px-3 py-2 border border-gray-300 rounded-md"
        />
      </div>
    </section>
  );
}
