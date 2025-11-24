'use client';
import Link from 'next/link';
import type { MouseEventHandler } from 'react';
import { FiPlusCircle } from 'react-icons/fi';

import { navBtn, icon20, textNav } from '@/src/styles/navClasses';

export default function CreateGameButton({ href, onClick }: { href: string; onClick?: MouseEventHandler<HTMLAnchorElement> | (() => void) }) {
  return (
    <Link href={href} className={navBtn} onClick={onClick as any}>
      <span className={icon20}><FiPlusCircle size={20} className="text-foreground" aria-hidden /></span>
      <span className={textNav}>Créer un match</span>
    </Link>
  );
}
