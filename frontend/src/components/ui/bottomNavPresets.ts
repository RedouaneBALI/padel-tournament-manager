import { Home as HomeIcon, Users as UsersIcon, Settings as SettingsIcon } from 'lucide-react';
import { FiMoreHorizontal, FiUsers, FiSettings } from 'react-icons/fi';
import { LuSwords } from 'react-icons/lu';
import { TbTournament } from 'react-icons/tb';
import type { BottomNavItem } from './BottomNav';

// Items par défaut (Accueil + Plus)
export const getDefaultBottomItems = (): BottomNavItem[] => [
  { href: '/', label: 'Accueil', Icon: HomeIcon, isActive: (p) => p === '/' },
  { href: '#more', label: 'Plus', Icon: FiMoreHorizontal },
];

// Items pour un tournoi admin
export const getAdminTournamentItems = (id: string): BottomNavItem[] => [
  { href: `/admin/tournament/${id}/players`, label: 'Joueurs', Icon: FiUsers },
  { href: `/admin/tournament/${id}/rounds/config`, label: 'Formats', Icon: FiSettings, isActive: (p) => p.includes(`/admin/tournament/${id}/rounds/config`) },
  { href: `/admin/tournament/${id}/games`, label: 'Matchs', Icon: LuSwords },
  { href: `/admin/tournament/${id}/bracket`, label: 'Tableau', Icon: TbTournament },
  { href: '#more', label: 'Plus', Icon: FiMoreHorizontal },
];

// Items pour un tournoi public
export const getPublicTournamentItems = (id: string): BottomNavItem[] => [
  { href: `/tournament/${id}`, label: 'Home', Icon: HomeIcon },
  { href: `/tournament/${id}/players`, label: 'Joueurs', Icon: UsersIcon },
  { href: `/tournament/${id}/games`, label: 'Matchs', Icon: LuSwords },
  { href: `/tournament/${id}/bracket`, label: 'Tableau', Icon: TbTournament },
  { href: '#more', label: 'Plus', Icon: FiMoreHorizontal },
];