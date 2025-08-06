// src/components/round/GroupStageResults.tsx
export default function GroupStageResults({ rounds }: { rounds: Round[] }) {
  console.log(rounds);
  return (
    <div className="space-y-8">
      {rounds.map(round =>
        round.groups?.map(group => (
          <div key={group.name} className="bg-white rounded shadow p-4">
            <h3 className="text-lg font-semibold mb-2">Groupe {group.name}</h3>

            {/* Affichage du classement */}
            <table className="w-full text-sm text-left text-gray-700 border border-gray-300">
              <thead>
                <tr className="bg-gray-100 text-gray-900">
                  <th className="p-2 border">#</th>
                  <th className="p-2 border">Équipe</th>
                  <th className="p-2 border">Pts</th>
                  <th className="p-2 border">Sets</th>
                  <th className="p-2 border">Jeux</th>
                </tr>
              </thead>
              <tbody>
                {group.poolRanking?.ranking?.map((entry, index) => (
                  <tr key={index}>
                    <td className="p-2 border">{index + 1}</td>
                    <td className="p-2 border">{entry.pair.name}</td>
                    <td className="p-2 border">{entry.points}</td>
                    <td className="p-2 border">{entry.setAverage}</td>
                    <td className="p-2 border">{entry.gameAverage}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))
      )}
    </div>
  );
}