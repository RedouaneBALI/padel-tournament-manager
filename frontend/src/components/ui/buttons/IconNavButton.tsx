'use client';

import React from 'react';
import Link from 'next/link';

interface Props {
  href: string;
  children?: React.ReactNode;
  onClick?: () => void;
  className?: string;
}

export default function IconNavButton({ href, children, onClick, className = '' }: Props) {
  return (
    <Link
      href={href}
      className={[
        'flex h-10 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground',
        className,
      ].join(' ')}
      onClick={onClick as any}
    >
      {children}
    </Link>
  );
}
