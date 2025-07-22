export default function TournamentDatesSection({ formData, handleInputChange }) {
  return (
    <section className="space-y-6">
      <h3 className="text-lg font-semibold text-gray-900">Dates du tournoi</h3>
      <div className="grid gap-6 md:grid-cols-2">
        <input
          id="startDate"
          name="startDate"
          type="date"
          value={formData.startDate}
          onChange={handleInputChange}
          className="w-full px-3 py-2 border rounded-md"
        />
        <input
          id="endDate"
          name="endDate"
          type="date"
          value={formData.endDate}
          onChange={handleInputChange}
          className="w-full px-3 py-2 border rounded-md"
        />
      </div>
    </section>
  );
}
