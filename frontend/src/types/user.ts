export enum ProfileType {
  PLAYER = 'PLAYER',
  SPECTATOR = 'SPECTATOR',
  ORGANIZER = 'ORGANIZER',
}

export interface User {
  id: number;
  email: string;
  name: string;
  locale: string;
  profileType?: ProfileType;
  city?: string;
  country?: string;
}
