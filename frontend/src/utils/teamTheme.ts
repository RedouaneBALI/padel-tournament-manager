export type TeamColor = 'TEAM_A' | 'TEAM_B';

export interface TeamThemeColors {
  button: {
    bg: string;
    hover: string;
    border: string;
    shadow: string;
  };
  point: {
    bg: string;
    text: string;
    border: string;
  };
}

const TEAM_A_THEME: TeamThemeColors = {
  button: {
    bg: 'bg-blue-600',
    hover: 'hover:bg-blue-700',
    border: 'border-blue-700',
    shadow: 'shadow-blue-200',
  },
  point: {
    bg: 'bg-blue-50',
    text: 'text-blue-700',
    border: 'border-blue-100',
  },
};

const TEAM_B_THEME: TeamThemeColors = {
  button: {
    bg: 'bg-rose-600',
    hover: 'hover:bg-rose-700',
    border: 'border-rose-700',
    shadow: 'shadow-rose-200',
  },
  point: {
    bg: 'bg-rose-50',
    text: 'text-rose-700',
    border: 'border-rose-100',
  },
};

export function getTeamTheme(isTeamA: boolean): TeamThemeColors {
  return isTeamA ? TEAM_A_THEME : TEAM_B_THEME;
}

export function getTeamThemeByColor(color: 'blue' | 'rose'): TeamThemeColors {
  return color === 'blue' ? TEAM_A_THEME : TEAM_B_THEME;
}

