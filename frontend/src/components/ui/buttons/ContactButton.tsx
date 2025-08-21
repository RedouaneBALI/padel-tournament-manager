'use client';
import Link from 'next/link';
import type { MouseEventHandler } from 'react';
import { FiMail } from 'react-icons/fi';

import { navBtn, icon20, textNav } from '@/src/styles/navClasses';

export default function ContactButton({ onClick }: { onClick?: MouseEventHandler<HTMLAnchorElement> | (() => void) }) {
  return (
    <Link href="/contact" className={navBtn} onClick={onClick as any}>
      <span className={icon20}><FiMail size={20} className="text-foreground" aria-hidden /></span>
      <span className={textNav}>Contact</span>
    </Link>
  );
}
