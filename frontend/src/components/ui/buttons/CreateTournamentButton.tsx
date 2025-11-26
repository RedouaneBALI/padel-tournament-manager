'use client';
import React from 'react';
import { FiPlusCircle } from 'react-icons/fi';

import IconNavButton from '@/src/components/ui/buttons/IconNavButton';
import { icon20, textNav } from '@/src/styles/navClasses';

export default function CreateTournamentButton({ href, onClick }: { href: string; onClick?: any }) {
  return (
    <IconNavButton href={href} onClick={onClick}>
      <span className={icon20}><FiPlusCircle size={20} className="text-foreground" aria-hidden /></span>
      <span className={textNav}>Créer un tournoi</span>
    </IconNavButton>
  );
}
