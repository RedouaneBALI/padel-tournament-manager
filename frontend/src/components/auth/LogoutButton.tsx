'use client';

import { signOut } from 'next-auth/react';
import { FiLogOut } from 'react-icons/fi';

interface LogoutButtonProps {
  children?: React.ReactNode;
}

export default function LogoutButton({ children }: LogoutButtonProps) {
  return (
    <button
      onClick={() => signOut({ callbackUrl: '/' })}
      className="flex items-center gap-2 text-sm text-muted hover:text-error"
    >
      <FiLogOut className="w-5 h-5" />
      {children ?? <span className="hidden md:inline">Déconnexion</span>}
    </button>
  );
}