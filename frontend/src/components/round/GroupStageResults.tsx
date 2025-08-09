import type { Round } from '@/src/types/round';

export default function GroupStageResults({
  rounds,
  nbQualifiedByPool,
}: {
  rounds: Round[];
  nbQualifiedByPool: number;
}) {
  return (
    <div className="space-y-4">
      {rounds.map(round =>
        round.pools?.map(pool => (
          <div key={pool.name} className="bg-white rounded shadow">
            <h3 className="text-lg font-semibold mb-2">Groupe {pool.name}</h3>

            <table className="w-full text-sm text-left text-gray-700 border border-gray-300">
              <thead>
                <tr className="bg-gray-100 text-gray-900">
                  <th className="p-2 border text-center">#</th>
                  <th className="p-2 border text-center">Équipe</th>
                  <th className="p-2 border text-center">Victoires</th>
                  <th className="p-2 border text-center">Diff. jeux</th>
                </tr>
              </thead>
              <tbody>
                {(pool.poolRanking?.details ?? []).map((entry, index) => (
                  <tr
                    key={index}
                    className={index < nbQualifiedByPool ? 'border-l-4 border-green-500' : ''}
                  >
                    <td className="p-2 border text-center">{index + 1}</td>
                    <td className="p-2 border text-center">
                      <div className="flex flex-col items-center">
                        <span>{entry.playerPair?.player1?.name}</span>
                        <span>{entry.playerPair?.player2?.name}</span>
                      </div>
                    </td>
                    <td className="p-2 border text-center">{entry.points}</td>
                    <td className="p-2 border text-center">{entry.setAverage}</td>
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