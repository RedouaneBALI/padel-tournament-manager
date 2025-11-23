'use client';

import Link from 'next/link';
import type { MouseEventHandler } from 'react';
import { Calculator } from 'lucide-react';

import { navBtn, icon20, textNav } from '@/src/styles/navClasses';

interface PointsCalculatorButtonProps {
  onClick?: MouseEventHandler<HTMLAnchorElement> | (() => void);
}

export default function PointsCalculatorButton({ onClick }: PointsCalculatorButtonProps) {
  return (
    <Link href="/calculateur-points" className={navBtn} onClick={onClick as any}>
      <span className={icon20}>
        <Calculator size={20} className="text-foreground" aria-hidden />
      </span>
      <span className={textNav}>Calculateur de points</span>
    </Link>
  );
}

