'use client';

import { signOut } from 'next-auth/react';
import { FiLogOut } from 'react-icons/fi';

export default function LogoutButton() {
  return (
    <button
      onClick={() => signOut()}
      className="flex items-center gap-2 text-sm text-red-600 hover:text-red-800"
    >
      <FiLogOut />
      Déconnexion
    </button>
  );
}