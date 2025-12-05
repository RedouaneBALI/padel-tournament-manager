'use client';

import Link from 'next/link';
import type { MouseEventHandler } from 'react';
import { Users } from 'lucide-react';

import { navBtn, icon20, textNav } from '@/src/styles/navClasses';

interface RankingButtonProps {
  onClick?: MouseEventHandler<HTMLAnchorElement> | (() => void);
}

export default function RankingButton({ onClick }: RankingButtonProps) {
  return (
    <Link href="/frmt/classement/hommes" className={navBtn} onClick={onClick as any}>
      <span className={icon20}>
        <Users size={20} className="text-foreground" aria-hidden />
      </span>
      <span className={textNav}>Classements</span>
    </Link>
  );
}
