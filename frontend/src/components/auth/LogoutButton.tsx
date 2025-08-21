'use client';

import { signOut } from 'next-auth/react';
import { FiLogOut } from 'react-icons/fi';

interface LogoutButtonProps {
  children?: React.ReactNode;
  className?: string;
  iconClassName?: string;
}

export default function LogoutButton({ children, iconClassName }: LogoutButtonProps) {
  return (
    <button onClick={() => signOut({ callbackUrl: '/' })} className="flex h-12 items-center gap-3 px-2 rounded hover:bg-accent hover:text-accent-foreground">
      <FiLogOut className="inline-flex w-5 h-5 items-center justify-center" />
      {children ?? <span className="text-sm">Déconnexion</span>}
    </button>
  );
}