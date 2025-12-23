'use client';

import Link from 'next/link';
import { FaRegStar } from 'react-icons/fa';

import { navBtn, textNav, icon20 } from '@/src/styles/navClasses';

interface Props {
  readonly onClick?: () => void;
  readonly className?: string;
}

export default function FavoritesButton({ onClick, className = '' }: Props) {
  return (
    <Link href="/favorites" onClick={onClick} className={`${navBtn} ${className}`}>
      <FaRegStar className={`${icon20} text-foreground`} />
      <span className={textNav}>Mes favoris</span>
    </Link>
  );
}
