'use client';

import { signIn } from 'next-auth/react';
import Button from '@/src/components/ui/buttons/Button';

interface Props {
  callbackUrl?: string;
  className?: string;
  onBeforeSignIn?: () => void;
}

export default function FacebookLoginButton({
  callbackUrl = '/admin/tournaments',
  className = '',
  onBeforeSignIn,
}: Props) {
  return (
    <Button
      variant="secondary"
      onClick={() => {
        if (onBeforeSignIn) {
          onBeforeSignIn();
        }
        signIn('facebook', { redirect: true, callbackUrl: '/auth/check-profile' });
      }}
      className={className}
      aria-label="Se connecter avec Facebook"
    >
      <img src="/facebook-logo.svg" alt="Facebook" className="w-5 h-5" />
      <span className="text-sm text-foreground">Se connecter avec Facebook</span>
    </Button>
  );
}
