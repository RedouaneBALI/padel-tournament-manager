'use client';
import Link from 'next/link';
import type { MouseEventHandler } from 'react';
import { TbPlayerPlay } from 'react-icons/tb';

import { navBtn, icon20, textNav } from '@/src/styles/navClasses';

export default function MyGamesButton({ href, onClick }: { href: string; onClick?: MouseEventHandler<HTMLAnchorElement> | (() => void) }) {
  return (
    <Link href={href} className={navBtn} onClick={onClick as any}>
      <span className={icon20}><TbPlayerPlay size={20} className="text-foreground" aria-hidden /></span>
      <span className={textNav}>Mes matchs</span>
    </Link>
  );
}

