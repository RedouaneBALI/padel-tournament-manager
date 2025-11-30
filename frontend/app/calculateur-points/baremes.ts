/**
 * Barèmes de points pour les tournois de padel
 * Source: https://www.frmt.ma/guide-de-la-competition-de-padel/
 */

export type TeamRangeKey = '8' | '9-12' | '13-16' | '17-20' | '21-24' | '25-28' | '29+';
export type TournamentLevelKey = 'P50' | 'P100' | 'P250' | 'P500' | 'P1000' | 'P1500' | 'P2000';

export interface TournamentLevel {
  value: TournamentLevelKey;
  label: string;
}

export interface TeamRange {
  value: TeamRangeKey;
  label: string;
}

export type BaremePoints = Record<TeamRangeKey, number[]>;

/**
 * Barème P50 - Tournoi de 50 points
 */
export const P50_BAREME: BaremePoints = {
  '8': [50, 30, 24, 18, 12, 8, 4, 2],
  '9-12': [50, 34, 30, 26, 22, 18, 14, 10, 8, 6, 4, 2],
  '13-16': [50, 36, 32, 30, 28, 26, 24, 22, 20, 18, 14, 10, 8, 6, 4, 2],
  '17-20': [50, 40, 36, 34, 32, 30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 8, 6, 4, 2],
  '21-24': [50, 40, 38, 36, 34, 32, 30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 8, 6, 4, 2, 2, 2, 2],
  '25-28': [50, 42, 38, 36, 34, 32, 30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 8, 6, 4, 2, 2, 2, 2, 2, 2, 2, 2],
  '29+': [50, 46, 42, 38, 36, 34, 32, 30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 6, 4, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2],
};

/**
 * Barème P100 - Tournoi de 100 points
 */
export const P100_BAREME: BaremePoints = {
  '8': [100, 60, 50, 40, 25, 10, 5, 1],
  '9-12': [100, 65, 55, 50, 35, 25, 20, 15, 10, 5, 3, 1],
  '13-16': [100, 70, 60, 55, 45, 40, 35, 30, 25, 21, 18, 15, 10, 5, 3, 1],
  '17-20': [100, 75, 65, 60, 55, 50, 45, 40, 35, 30, 25, 23, 20, 18, 15, 12, 10, 5, 3, 1],
  '21-24': [100, 75, 70, 65, 60, 55, 50, 47, 43, 40, 37, 33, 30, 28, 25, 23, 20, 18, 15, 12, 10, 5, 3, 1],
  '25-28': [100, 80, 75, 70, 65, 60, 55, 53, 50, 48, 45, 43, 40, 38, 35, 33, 30, 28, 25, 23, 20, 18, 15, 12, 10, 5, 3, 1],
  '29+': [100, 80, 75, 72, 70, 65, 63, 60, 58, 55, 53, 50, 48, 45, 43, 40, 38, 35, 33, 30, 28, 25, 23, 20, 18, 17, 16, 15, 14, 13, 12, 11, 10, 10, 10, 10, 8, 8, 8, 8, 5, 5, 5, 5, 5, 5, 5, 5],
};

/**
 * Barème P250 - Tournoi de 250 points
 */
export const P250_BAREME: BaremePoints = {
  '8': [250, 150, 125, 100, 63, 25, 13, 3],
  '9-12': [250, 163, 138, 125, 88, 63, 50, 38, 25, 13, 8, 3],
  '13-16': [250, 175, 150, 138, 113, 100, 88, 75, 63, 53, 45, 38, 25, 13, 8, 3],
  '17-20': [250, 188, 163, 150, 138, 125, 113, 100, 88, 75, 63, 58, 50, 45, 38, 30, 25, 13, 8, 3],
  '21-24': [250, 188, 175, 163, 150, 138, 125, 118, 108, 100, 93, 83, 75, 70, 63, 58, 50, 45, 38, 30, 25, 13, 8, 3],
  '25-28': [250, 200, 188, 175, 163, 150, 138, 133, 125, 120, 113, 108, 100, 95, 88, 83, 75, 70, 63, 58, 50, 45, 38, 30, 25, 13, 8, 3],
  '29+': [250, 200, 188, 180, 175, 163, 158, 150, 145, 138, 133, 125, 120, 113, 108, 100, 95, 88, 83, 75, 70, 63, 58, 55, 45, 38, 33, 30, 28, 25, 23, 20, 17, 17, 17, 17, 15, 15, 15, 15, 10, 10, 10, 10, 10, 10, 10, 10],
};

/**
 * Barème P500 - Tournoi de 500 points
 * Note: Le P500 ne propose pas de tournois avec 8 équipes
 */
export const P500_BAREME: BaremePoints = {
  '8': [], // Pas de tournois P500 avec 8 équipes
  '9-12': [500, 325, 275, 250, 175, 125, 100, 75, 50, 25, 15, 5],
  '13-16': [500, 350, 300, 275, 225, 200, 175, 150, 125, 105, 90, 75, 50, 25, 15, 5],
  '17-20': [500, 375, 325, 300, 275, 250, 225, 200, 175, 150, 125, 115, 100, 90, 75, 60, 50, 25, 15, 5],
  '21-24': [500, 375, 350, 325, 300, 275, 250, 235, 215, 200, 185, 165, 150, 140, 125, 115, 100, 90, 75, 60, 50, 25, 15, 5],
  '25-28': [500, 400, 375, 350, 325, 300, 275, 265, 250, 240, 225, 215, 200, 190, 175, 165, 150, 140, 125, 115, 100, 90, 75, 60, 50, 25, 15, 5],
  '29+': [500, 400, 375, 360, 350, 325, 315, 300, 290, 275, 265, 250, 240, 225, 215, 200, 190, 175, 165, 150, 140, 125, 115, 100, 90, 75, 65, 60, 55, 50, 45, 40, 35, 35, 35, 35, 25, 25, 25, 25, 15, 15, 15, 15, 15, 15, 15, 15],
};

/**
 * Barème P1000 - Tournoi de 1000 points
 * Note: Le P1000 ne propose pas de tournois avec moins de 17 équipes
 */
export const P1000_BAREME: BaremePoints = {
  '8': [], // Pas de tournois P1000 avec 8 équipes
  '9-12': [], // Pas de tournois P1000 avec 9-12 équipes
  '13-16': [], // Pas de tournois P1000 avec 13-16 équipes
  '17-20': [1000, 750, 700, 650, 600, 550, 500, 470, 430, 400, 370, 330, 300, 280, 250, 230, 200, 180, 150, 120],
  '21-24': [1000, 750, 700, 650, 600, 550, 500, 470, 430, 400, 370, 330, 300, 280, 250, 230, 200, 180, 150, 120, 100, 80, 70, 50],
  '25-28': [1000, 800, 750, 720, 700, 650, 630, 600, 580, 550, 530, 500, 480, 450, 430, 400, 380, 350, 330, 300, 280, 250, 230, 200, 180, 150, 130, 110],
  '29+': [1000, 800, 750, 720, 700, 650, 630, 600, 580, 550, 530, 500, 480, 450, 430, 400, 380, 350, 330, 300, 280, 250, 230, 200, 180, 150, 130, 110, 100, 85, 80, 75, 60, 60, 60, 60, 40, 40, 40, 40, 25, 25, 25, 25, 25, 25, 25, 25],
};

/**
 * Barème P1500 - Tournoi de 1500 points
 * Note: Le P1500 ne propose pas de tournois avec moins de 17 équipes
 */
export const P1500_BAREME: BaremePoints = {
  '8': [], // Pas de tournois P1500 avec 8 équipes
  '9-12': [], // Pas de tournois P1500 avec 9-12 équipes
  '13-16': [], // Pas de tournois P1500 avec 13-16 équipes
  '17-20': [1500, 1100, 1000, 850, 750, 670, 600, 550, 500, 450, 420, 400, 370, 350, 320, 300, 270, 250, 200, 170],
  '21-24': [1500, 1100, 1000, 850, 750, 670, 600, 550, 500, 450, 420, 400, 370, 350, 320, 300, 270, 250, 200, 170, 150, 120, 100, 90],
  '25-28': [1500, 1200, 1100, 1000, 900, 850, 800, 750, 700, 670, 650, 620, 600, 570, 550, 520, 500, 470, 450, 420, 400, 370, 350, 320, 300, 270, 250, 220],
  '29+': [1500, 1200, 1100, 1000, 900, 850, 800, 750, 700, 670, 650, 620, 600, 570, 550, 520, 500, 470, 450, 420, 400, 370, 350, 320, 300, 270, 250, 220, 200, 170, 150, 120, 90, 90, 90, 90, 70, 70, 70, 70, 50, 50, 50, 50, 50, 50, 50, 50],
};

/**
 * Barème P2000 - Tournoi de 2000 points
 * Note: Le P2000 ne propose pas de tournois avec moins de 17 équipes
 */
export const P2000_BAREME: BaremePoints = {
  '8': [], // Pas de tournois P2000 avec 8 équipes
  '9-12': [], // Pas de tournois P2000 avec 9-12 équipes
  '13-16': [], // Pas de tournois P2000 avec 13-16 équipes
  '17-20': [2000, 1450, 1300, 1150, 1000, 930, 870, 810, 750, 720, 690, 630, 600, 570, 540, 510, 490, 460, 430, 400],
  '21-24': [2000, 1450, 1300, 1150, 1000, 930, 870, 810, 750, 720, 690, 630, 600, 570, 540, 510, 490, 460, 430, 400, 370, 340, 310, 280],
  '25-28': [2000, 1600, 1450, 1300, 1150, 1050, 1000, 960, 930, 900, 870, 840, 810, 780, 750, 720, 690, 660, 630, 600, 570, 540, 510, 490, 460, 430, 400, 370],
  '29+': [2000, 1600, 1450, 1300, 1150, 1050, 1000, 960, 930, 900, 870, 840, 810, 780, 750, 720, 690, 660, 630, 600, 570, 540, 510, 490, 460, 430, 400, 370, 340, 310, 280, 250, 160, 160, 160, 160, 120, 120, 120, 120, 80, 80, 80, 80, 80, 80, 80, 80],
};

/**
 * Tous les barèmes de points par niveau de tournoi
 */
export const BAREMES: Record<TournamentLevelKey, BaremePoints> = {
  P50: P50_BAREME,
  P100: P100_BAREME,
  P250: P250_BAREME,
  P500: P500_BAREME,
  P1000: P1000_BAREME,
  P1500: P1500_BAREME,
  P2000: P2000_BAREME,
};

/**
 * Liste des niveaux de tournois disponibles
 */
export const TOURNAMENT_LEVELS: TournamentLevel[] = [
  { value: 'P50', label: 'P50' },
  { value: 'P100', label: 'P100' },
  { value: 'P250', label: 'P250' },
  { value: 'P500', label: 'P500' },
  { value: 'P1000', label: 'P1000' },
  { value: 'P1500', label: 'P1500' },
  { value: 'P2000', label: 'P2000' },
];

/**
 * Liste des fourchettes d'équipes disponibles
 */
export const TEAM_RANGES: TeamRange[] = [
  { value: '8', label: '8 équipes' },
  { value: '9-12', label: '9 à 12 équipes' },
  { value: '13-16', label: '13 à 16 équipes' },
  { value: '17-20', label: '17 à 20 équipes' },
  { value: '21-24', label: '21 à 24 équipes' },
  { value: '25-28', label: '25 à 28 équipes' },
  { value: '29+', label: '29 équipes ou plus' },
];

/**
 * Fonction helper pour obtenir les points d'un classement spécifique
 */
export function getPoints(
  tournamentLevel: TournamentLevelKey,
  teamRange: TeamRangeKey,
  ranking: number
): number | null {
  const bareme = BAREMES[tournamentLevel]?.[teamRange];
  if (!bareme || bareme.length === 0) return null;

  const index = ranking - 1;
  if (index < 0) return null;
  if (index >= bareme.length) return bareme[bareme.length - 1];

  return bareme[index];
}

/**
 * Fonction helper pour vérifier si une combinaison niveau/équipes est valide
 */
export function isValidCombination(
  tournamentLevel: TournamentLevelKey,
  teamRange: TeamRangeKey
): boolean {
  const bareme = BAREMES[tournamentLevel]?.[teamRange];
  return bareme !== undefined && bareme.length > 0;
}

/**
 * Configuration des multiplicateurs de points selon le nombre d'équipes Top 100
 * Pour les tournois P250, P500, P1000, P1500, P2000
 */
export interface Top100TeamOption {
  value: number;
  label: string;
  multiplier: number;
}

export interface Top100TeamConfig {
  minTeams: number;
  maxTeams: number;
  options: Top100TeamOption[];
}

export const TOP_100_CONFIGS: Record<string, Top100TeamConfig> = {
  P250: {
    minTeams: 4,
    maxTeams: 8,
    options: [
      { value: 8, label: '8 équipes Top 500 (100% des points)', multiplier: 1.0 },
      { value: 7, label: '7 équipes Top 500 (90% des points)', multiplier: 0.9 },
      { value: 6, label: '6 équipes Top 500 (80% des points)', multiplier: 0.8 },
      { value: 5, label: '5 équipes Top 500 (70% des points)', multiplier: 0.7 },
      { value: 4, label: '4 équipes Top 500 (60% des points)', multiplier: 0.6 },
    ],
  },
  P500: {
    minTeams: 4,
    maxTeams: 8,
    options: [
      { value: 8, label: '8 équipes Top 200 (100% des points)', multiplier: 1.0 },
      { value: 7, label: '7 équipes Top 200 (90% des points)', multiplier: 0.9 },
      { value: 6, label: '6 équipes Top 200 (80% des points)', multiplier: 0.8 },
      { value: 5, label: '5 équipes Top 200 (70% des points)', multiplier: 0.7 },
      { value: 4, label: '4 équipes Top 200 (60% des points)', multiplier: 0.6 },
    ],
  },
  P1000: {
    minTeams: 4,
    maxTeams: 8,
    options: [
      { value: 8, label: '8 équipes Top 100 (100% des points)', multiplier: 1.0 },
      { value: 7, label: '7 équipes Top 100 (90% des points)', multiplier: 0.9 },
      { value: 6, label: '6 équipes Top 100 (80% des points)', multiplier: 0.8 },
      { value: 5, label: '5 équipes Top 100 (70% des points)', multiplier: 0.7 },
      { value: 4, label: '4 équipes Top 100 (60% des points)', multiplier: 0.6 },
    ],
  },
  P1500: {
    minTeams: 8,
    maxTeams: 12,
    options: [
      { value: 12, label: '12 équipes Top 100 (100% des points)', multiplier: 1.0 },
      { value: 11, label: '11 équipes Top 100 (90% des points)', multiplier: 0.9 },
      { value: 10, label: '10 équipes Top 100 (80% des points)', multiplier: 0.8 },
      { value: 9, label: '9 équipes Top 100 (70% des points)', multiplier: 0.7 },
      { value: 8, label: '8 équipes Top 100 (60% des points)', multiplier: 0.6 },
    ],
  },
  P2000: {
    minTeams: 8,
    maxTeams: 12,
    options: [
      { value: 12, label: '12 équipes Top 100 (100% des points)', multiplier: 1.0 },
      { value: 11, label: '11 équipes Top 100 (90% des points)', multiplier: 0.9 },
      { value: 10, label: '10 équipes Top 100 (80% des points)', multiplier: 0.8 },
      { value: 9, label: '9 équipes Top 100 (70% des points)', multiplier: 0.7 },
      { value: 8, label: '8 équipes Top 100 (60% des points)', multiplier: 0.6 },
    ],
  },
};

/**
 * Configuration spécifique pour les tournois féminins : nombre de joueuses Top 10
 * On ne supporte que pour les niveaux >= P250
 */
export const TOP_FEMALE_TOP10_CONFIGS: Record<string, Top100TeamConfig> = {
  P250: {
    minTeams: 4,
    maxTeams: 8,
    options: [
      { value: 8, label: '8 joueuses Top 10 (100% des points)', multiplier: 1.0 },
      { value: 7, label: '7 joueuses Top 10 (90% des points)', multiplier: 0.9 },
      { value: 6, label: '6 joueuses Top 10 (80% des points)', multiplier: 0.8 },
      { value: 5, label: '5 joueuses Top 10 (70% des points)', multiplier: 0.7 },
      { value: 4, label: '4 joueuses Top 10 (60% des points)', multiplier: 0.6 },
    ],
  },
  P500: {
    minTeams: 3,
    maxTeams: 6,
    options: [
      { value: 6, label: '6 joueuses Top 10 (100% des points)', multiplier: 1.0 },
      { value: 5, label: '5 joueuses Top 10 (90% des points)', multiplier: 0.9 },
      { value: 4, label: '4 joueuses Top 10 (75% des points)', multiplier: 0.75 },
      { value: 3, label: '3 joueuses Top 10 (60% des points)', multiplier: 0.6 },
    ],
  },
  P1000: {
    minTeams: 4,
    maxTeams: 8,
    options: [
      { value: 8, label: '8 joueuses Top 10 (100% des points)', multiplier: 1.0 },
      { value: 7, label: '7 joueuses Top 10 (90% des points)', multiplier: 0.9 },
      { value: 6, label: '6 joueuses Top 10 (80% des points)', multiplier: 0.8 },
      { value: 5, label: '5 joueuses Top 10 (70% des points)', multiplier: 0.7 },
      { value: 4, label: '4 joueuses Top 10 (60% des points)', multiplier: 0.6 },
    ],
  },
  P1500: {
    minTeams: 3,
    maxTeams: 6,
    options: [
      { value: 6, label: '6 joueuses Top 10 (100% des points)', multiplier: 1.0 },
      { value: 5, label: '5 joueuses Top 10 (90% des points)', multiplier: 0.9 },
      { value: 4, label: '4 joueuses Top 10 (75% des points)', multiplier: 0.75 },
      { value: 3, label: '3 joueuses Top 10 (60% des points)', multiplier: 0.6 },
    ],
  },
  P2000: {
    minTeams: 3,
    maxTeams: 6,
    options: [
      { value: 6, label: '6 joueuses Top 10 (100% des points)', multiplier: 1.0 },
      { value: 5, label: '5 joueuses Top 10 (90% des points)', multiplier: 0.9 },
      { value: 4, label: '4 joueuses Top 10 (75% des points)', multiplier: 0.75 },
      { value: 3, label: '3 joueuses Top 10 (60% des points)', multiplier: 0.6 },
    ],
  },
};

/**
 * Fonction helper pour vérifier si un niveau de tournoi nécessite la sélection du nombre d'équipes Top 100
 */
export function requiresTop100Selection(level: string): boolean {
  return ['P250', 'P500', 'P1000', 'P1500', 'P2000'].includes(level);
}

/**
 * Fonction helper pour obtenir le multiplicateur de points selon le nombre d'équipes Top 100
 */
export function getTop100Multiplier(level: string, topCount: number | null | undefined, gender: 'M' | 'F' = 'M'): number {
  if (!requiresTop100Selection(level) || !topCount) {
    return 1.0;
  }

  const config = gender === 'F' ? TOP_FEMALE_TOP10_CONFIGS[level] : TOP_100_CONFIGS[level];
  if (!config) return 1.0;

  const option = config.options.find(opt => opt.value === topCount);
  return option ? option.multiplier : 1.0;
}

/**
 * Fonction helper pour appliquer le multiplicateur Top 100 aux points
 */
export function applyTop100Multiplier(basePoints: number, multiplier: number): number {
  return Math.round(basePoints * multiplier);
}

/**
 * Fonction helper pour obtenir la configuration Top 100 appropriée selon le niveau et le genre
 */
export function getTopConfig(level: string, gender: 'M' | 'F' = 'M'): Top100TeamConfig | undefined {
  if (!requiresTop100Selection(level)) return undefined;
  return gender === 'F' ? TOP_FEMALE_TOP10_CONFIGS[level] : TOP_100_CONFIGS[level];
}
