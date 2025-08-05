export enum Stage {
  Q1 = 'Q1',
  Q2 = 'Q2',
  R64 = 'R64',
  R32 = 'R32',
  R16 = 'R16',
  QUARTERS = 'QUARTERS',
  SEMIS = 'SEMIS',
  FINAL = 'FINAL',
  WINNER = 'WINNER',
}

export const stageLabels: Record<Stage, string> = {
  GROUPS: 'Phase de groupe',
  Q1: 'Qualifications 1',
  Q2: 'Qualifications 2',
  R64: '1/32 de finale',
  R32: '1/16 de finale',
  R16: '1/8 de finale',
  QUARTERS: 'Quart de finale',
  SEMIS: 'Demi-finale',
  FINAL: 'Finale',
  WINNER: 'Vainqueur',
};