'use client';

import Link from 'next/link';
import { MdOutlineAttachMoney } from 'react-icons/md';

interface Props {
  onClick?: () => void;
  className?: string;
}

export default function PricingButton({ onClick, className = '' }: Props) {
  return (
    <Link
      href="/pricing"
      onClick={onClick}
      className={[
        'flex h-12 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground',
        className,
      ].join(' ')}
    >
      <MdOutlineAttachMoney className="w-5 h-5 flex-none" />
      <span className="text-sm">Pricing</span>
    </Link>
  );
}