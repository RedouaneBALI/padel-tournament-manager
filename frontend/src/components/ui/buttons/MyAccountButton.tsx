'use client';
import Link from 'next/link';
import type { MouseEventHandler } from 'react';
import { FiUser } from 'react-icons/fi';

import { navBtn, icon20, textNav } from '@/src/styles/navClasses';

export default function MyAccountButton({ onClick }: { onClick?: MouseEventHandler<HTMLAnchorElement> | (() => void) }) {
  return (
    <Link href="/mon-compte" className={navBtn} onClick={onClick as any}>
      <span className={icon20}><FiUser size={20} className="text-foreground" aria-hidden /></span>
      <span className={textNav}>Mon compte</span>
    </Link>
  );
}
