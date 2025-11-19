import { PlayerPair } from '@/src/types/playerPair';

// Helper to build a BYE pair placeholder
export const makeByePair = (): PlayerPair => ({
  player1Name: 'BYE',
  player2Name: '',
  type: 'BYE'
});

/**
 * Applies BYE positioning logic using bye-positions.json
 * @param playerPairs - The list of player pairs to reorder
 * @param slotsSize - The total size of the draw (must be power of 2: 8, 16, 32, 64)
 * @returns A promise that resolves to the reordered array of slots, or null if error
 */
export async function applyByePositions(
  playerPairs: PlayerPair[],
  slotsSize: number
): Promise<Array<PlayerPair | null> | null> {
  try {
    // Determine number of BYEs from the provided playerPairs (type === 'BYE')
    const byePairs = (playerPairs || []).filter((p) => p?.type === 'BYE');
    const byesCount = byePairs.length;

    if (byesCount === 0) {
      window.alert('Aucun BYE trouvé parmi les paires.');
      return null;
    }

    // Fetch positions mapping
    const resp = await fetch('/bye-positions.json');
    if (!resp.ok) {
      window.alert('Impossible de charger la configuration des BYE.');
      return null;
    }
    const mapping = await resp.json();

    const drawKey = String(slotsSize);
    const drawObj = mapping[drawKey];
    if (!drawObj) {
      window.alert(`Pas de configuration BYE pour un tableau de taille ${slotsSize}`);
      return null;
    }

    // Directly access positions by number of BYEs
    const positions = drawObj[String(byesCount)];
    if (!Array.isArray(positions) || positions.length === 0) {
      window.alert('Positions BYE non définies pour ce nombre de BYE.');
      return null;
    }

    // positions are expected as zero-based indices
    const result: Array<PlayerPair | null> = new Array(slotsSize).fill(null);

    // use existing bye pair objects if available, otherwise create placeholders
    const byeQueue = [...byePairs];

    for (let i = 0; i < positions.length; i++) {
      const idx = Number(positions[i]);
      const bye = byeQueue.shift() || makeByePair();
      if (idx >= 0 && idx < slotsSize) {
        result[idx] = bye;
      }
    }

    // fill remaining slots with non-BYE pairs in original order
    const nonByePairs = (playerPairs || []).filter((p) => p?.type !== 'BYE');
    let ni = 0;
    for (let i = 0; i < slotsSize; i++) {
      if (result[i] == null) {
        const next = nonByePairs[ni++];
        result[i] = next || null;
      }
    }

    return result;
  } catch (err) {
    console.error('Erreur lors de l\'application des BYE', err);
    window.alert('Erreur lors de l\'application des BYE');
    return null;
  }
}

