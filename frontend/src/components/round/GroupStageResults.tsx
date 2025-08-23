import type { Round } from '@/src/types/round';

export default function GroupStageResults({
  rounds,
  nbQualifiedByPool,
}: {
  rounds: Round[];
  nbQualifiedByPool: number;
}) {
  return (
    <div>
      {rounds.map(round =>
        round.pools?.map(pool => (
          <div key={pool.name} className="rounded-lg bg-background md:max-w-4xl md:mx-auto">
            <h3 className="text-lg font-semibold mt-3">Groupe {pool.name}</h3>

            <table className="w-full text-sm text-left text-foreground">
              <thead>
                <tr className="text-foreground">
                  <th className="p-2 text-center border border-border">#</th>
                  <th className="p-2 text-center border border-border">Équipe</th>
                  <th className="p-2 text-center border border-border">Victoires</th>
                  <th className="p-2 text-center border border-border">Diff. jeux</th>
                </tr>
              </thead>
              <tbody>
                {(pool.poolRanking?.details ?? []).map((entry, index) => (
                  <tr
                    key={index}
                    className={index < nbQualifiedByPool ? 'border-l-4 border-green-500' : ''}
                  >
                    <td className="p-2 text-center border border-border">{index + 1}</td>
                    <td className="p-2 text-center border border-border">
                      <div className="flex flex-col items-center">
                        <span>{entry.playerPair?.player1Name}</span>
                        <span>{entry.playerPair?.player2Name}</span>
                      </div>
                    </td>
                    <td className="p-2 text-center border border-border">{entry.points}</td>
                    <td className="p-2 text-center border border-border">{entry.setAverage}</td>
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