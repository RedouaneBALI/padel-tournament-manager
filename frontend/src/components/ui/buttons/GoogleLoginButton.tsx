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
  const handleSignIn = () => {
    if (onBeforeSignIn) {
      onBeforeSignIn();
    }

    // Store the callback URL for later use
    if (typeof window !== 'undefined') {
      localStorage.setItem('authReturnUrl', callbackUrl);
    }

    signIn('google', {
      redirect: true,
      callbackUrl: '/auth/check-profile'
    });
  };

  return (
    <Button
      variant="secondary"
      onClick={handleSignIn}
      className={className}
      aria-label="Se connecter avec Google"
    >
      <img src="/google-logo.svg" alt="Google" className="w-5 h-5" />
      <span className="text-sm text-foreground">Se connecter avec Google</span>
    </Button>
  );
}
