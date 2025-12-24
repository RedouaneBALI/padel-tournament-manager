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
          console.log('[GoogleLoginButton] localStorage.authReturnUrl:', localStorage.getItem('authReturnUrl'));
        } finally {
          console.log('[GoogleLoginButton] Calling signIn with callbackUrl: /auth/check-profile');
          signIn('google', { redirect: true, callbackUrl: '/auth/check-profile' });
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
