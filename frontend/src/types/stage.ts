export enum Stage {
  GROUPS = 'GROUPS',  // Utiliser des valeurs cohérentes
  Q1 = 'Q1',
  Q2 = 'Q2',
  Q3 = 'Q3',
  R64 = 'R64',
  R32 = 'R32',
  R16 = 'R16',
  QUARTERS = 'QUARTERS',
  SEMIS = 'SEMIS',
  FINAL = 'FINAL',
  WINNER = 'WINNER',
}

export const stageLabels: Record<Stage, string> = {
  GROUPS: 'Groupes',
  Q1: 'Qualifications 1',
  Q2: 'Qualifications 2',
  Q3: 'Qualifications 3',
  R64: '1/32 de finale',
  R32: '1/16 de finale',
  R16: '1/8 de finale',
  QUARTERS: 'Quart de finale',
  SEMIS: 'Demi-finale',
  FINAL: 'Finale',
  WINNER: 'Vainqueur',
};

export const getStageFromSize = (size: number): Stage => {
  if (size === 64) return Stage.R64;
  if (size === 32) return Stage.R32;
  if (size === 16) return Stage.R16;
  if (size === 8) return Stage.QUARTERS;
  if (size === 4) return Stage.SEMIS;
  if (size === 2) return Stage.FINAL;
  // Pour d'autres tailles, approximation à la valeur la plus proche
  if (size > 32) return Stage.R64;
  if (size > 16) return Stage.R32;
  if (size > 8) return Stage.R16;
  if (size > 4) return Stage.QUARTERS;
  if (size > 2) return Stage.SEMIS;
  return Stage.FINAL;
};
