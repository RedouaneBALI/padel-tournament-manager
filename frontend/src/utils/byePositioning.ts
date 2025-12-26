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
    // 1. Determine number of BYEs to place
    const byePairs = (playerPairs || []).filter((p) => p?.type === 'BYE');
    const byesCount = byePairs.length;

    if (byesCount === 0) {
      window.alert('Aucun BYE trouvé parmi les paires.');
      return null;
    }

    // 2. Fetch positions mapping
    const resp = await fetch('/bye-positions.json');
    if (!resp.ok) {
      window.alert('Impossible de charger la configuration des BYE.');
      return null;
    }
    const mapping = await resp.json();

    // 3. Get the prioritized list for this draw size
    // Expecting mapping to be Record<string, number[]>
    const fullPositions = mapping[String(slotsSize)];

    if (!Array.isArray(fullPositions) || fullPositions.length === 0) {
      window.alert(`Pas de configuration BYE pour un tableau de taille ${slotsSize}`);
      return null;
    }

    // 4. Select the exact positions needed
    // We simply take the first N positions, where N is the number of BYEs.
    // This works for any number of BYEs (even odd numbers like 3 or 5),
    // adhering to the priority order defined in the JSON.
    const targetPositions = fullPositions.slice(0, byesCount);

    // 5. Initialize result array
    const result: Array<PlayerPair | null> = new Array(slotsSize).fill(null);
    const byeQueue = [...byePairs];

    // 6. Place BYEs at their target positions
    targetPositions.forEach((posIndex) => {
      const idx = Number(posIndex);
      // Safety check to ensure index is within bounds and we have BYEs left
      if (idx >= 0 && idx < slotsSize && byeQueue.length > 0) {
        // We take one BYE from the queue and place it
        result[idx] = byeQueue.shift() as PlayerPair;
      }
    });

    // 7. Fill remaining slots
    // "fillers" contains any BYEs that might not have had a defined position (unlikely)
    // plus all normal teams.
    const nonByePairs = (playerPairs || []).filter((p) => p?.type !== 'BYE');
    const fillers = [...byeQueue, ...nonByePairs];

    let fillerIndex = 0;
    for (let i = 0; i < slotsSize; i++) {
      if (result[i] == null) {
        result[i] = fillers[fillerIndex++] || null;
      }
    }

    return result;
  } catch (err) {
    console.error('Erreur lors de l\'application des BYE', err);
    window.alert('Erreur lors de l\'application des BYE');
    return null;
  }
}