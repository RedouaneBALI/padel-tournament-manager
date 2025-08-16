'use client';
import Link from 'next/link';
import type { MouseEventHandler } from 'react';
import { TbTrophy } from 'react-icons/tb';

export default function MyTournamentsButton({ onClick }: { onClick?: MouseEventHandler<HTMLAnchorElement> | (() => void) }) {
  return (
    <Link
      href="/admin/tournaments"
      className="flex h-12 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground"
      onClick={onClick as any}
    >
      <span className="inline-flex w-5 h-5 items-center justify-center"><TbTrophy size={20} className="text-foreground" aria-hidden /></span>
      <span className="text-sm">Mes tournois</span>
    </Link>
  );
}
