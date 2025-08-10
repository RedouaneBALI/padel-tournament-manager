'use client';

import { signOut } from 'next-auth/react';
import { FiLogOut } from 'react-icons/fi';

export default function LogoutButton() {
  return (
    <button
      onClick={() => signOut()}
      className="flex items-center gap-2 text-sm text-muted hover:text-error"
    >
      <FiLogOut className="w-5 h-5" />
      Déconnexion
    </button>
  );
}