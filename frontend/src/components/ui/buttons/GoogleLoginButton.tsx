'use client';

import { signIn } from 'next-auth/react';
import SecondaryButton from '@/src/components/ui/buttons/SecondaryButton';

interface Props {
  callbackUrl?: string;
  className?: string;
  onBeforeSignIn?: () => void;
}

export default function GoogleLoginButton({
  callbackUrl = '/admin/tournaments',
  className = '',
  onBeforeSignIn,
}: Props) {
  return (
    <SecondaryButton
      onClick={() => {
        try {
          onBeforeSignIn?.();
        } finally {
          signIn('google', { redirect: true, callbackUrl });
        }
      }}
      className={className}
      ariaLabel="Se connecter avec Google"
    >
      <img src="/google-logo.svg" alt="Google" className="w-5 h-5" />
      <span className="text-sm text-foreground">Se connecter avec Google</span>
    </SecondaryButton>
  );
}
