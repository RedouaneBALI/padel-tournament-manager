'use client';

import Link from 'next/link';
import { MdOutlineAttachMoney } from 'react-icons/md';

import { navBtn, textNav, icon20 } from '@/src/styles/navClasses';

interface Props {
  onClick?: () => void;
  className?: string;
}

export default function PricingButton({ onClick, className = '' }: Props) {
  return (
    <Link href="/pricing" onClick={onClick} className={`${navBtn} ${className}`}>
      <MdOutlineAttachMoney className={`${icon20} text-foreground`} />
      <span className={textNav}>Pricing</span>
    </Link>
  );
}