'use client';

import { signOut } from 'next-auth/react';
import { FiLogOut } from 'react-icons/fi';

import { navBtn, icon20 } from '@/src/styles/navClasses';

interface LogoutButtonProps {
  children?: React.ReactNode;
  className?: string;
  iconClassName?: string;
}

export default function LogoutButton({ children, iconClassName }: LogoutButtonProps) {
  return (
    <button onClick={() => signOut({ callbackUrl: '/' })} className={navBtn}>
      <FiLogOut className={icon20} />
      {children ?? <span className="text-sm">Déconnexion</span>}
    </button>
  );
}