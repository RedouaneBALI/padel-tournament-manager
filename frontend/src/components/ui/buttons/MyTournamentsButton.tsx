'use client';
import Link from 'next/link';
import type { MouseEventHandler } from 'react';
import { TbTrophy } from 'react-icons/tb';

import { navBtn, icon20, textNav } from '@/src/styles/navClasses';

export default function MyTournamentsButton({ href, onClick }: { href: string; onClick?: MouseEventHandler<HTMLAnchorElement> | (() => void) }) {
  return (
    <Link href={href} className={navBtn} onClick={onClick as any}>
      <span className={icon20}><TbTrophy size={20} className="text-foreground" aria-hidden /></span>
      <span className={textNav}>Mes tournois</span>
    </Link>
  );
}
