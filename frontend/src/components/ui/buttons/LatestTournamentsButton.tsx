'use client';

import Link from 'next/link';
import { Trophy } from 'lucide-react';

import { navBtn, textNav, icon20 } from '@/src/styles/navClasses';

interface Props {
  onClick?: () => void;
  className?: string;
}

export default function LatestTournamentsButton({ onClick, className = '' }: Props) {
  return (
    <Link href="/community/organisateurs" onClick={onClick} className={`${navBtn} ${className}`}>
      <Trophy className={`${icon20} text-foreground`} />
      <span className={textNav}>Derniers tournois</span>
    </Link>
  );
}
