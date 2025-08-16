'use client';
import Link from 'next/link';
import type { MouseEventHandler } from 'react';
import { FiMail } from 'react-icons/fi';

export default function ContactButton({ onClick }: { onClick?: MouseEventHandler<HTMLAnchorElement> | (() => void) }) {
  return (
    <Link
      href="/contact"
      className="flex h-12 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground"
      onClick={onClick as any}
    >
      <span className="inline-flex w-5 h-5 items-center justify-center"><FiMail size={20} className="text-foreground" aria-hidden /></span>
      <span className="text-sm">Contact</span>
    </Link>
  );
}
