'use client';

import { signIn } from 'next-auth/react';
import Button from '@/src/components/ui/buttons/Button';

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
    <Button
      variant="secondary"
      onClick={() => {
        try {
          onBeforeSignIn?.();
        } finally {
          signIn('google', { redirect: true, callbackUrl });
        }
      }}
      className={className}
      aria-label="Se connecter avec Google"
    >
      <img src="/google-logo.svg" alt="Google" className="w-5 h-5" />
      <span className="text-sm text-foreground">Se connecter avec Google</span>
    </Button>
  );
}
