'use client';

import Link from 'next/link';
import { FiImage } from 'react-icons/fi';

import { navBtn, icon20, textNav } from '@/src/styles/navClasses';

export default function CommunityButton({ onClick }: { onClick?: any }) {
  return (
    <Link href="/community/organisateurs" className={navBtn} onClick={onClick as any}>
      <span className={icon20}><FiImage size={20} className="text-foreground" aria-hidden /></span>
      <span className={textNav}>Témoignages</span>
    </Link>
  );
}
