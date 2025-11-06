'use client';

import { signIn } from 'next-auth/react';

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
    <button
      type="button"
      onClick={() => {
        try {
          onBeforeSignIn?.();
        } finally {
          signIn('google', { redirect: true, callbackUrl });
        }
      }}
      className={[
        'flex items-center justify-center gap-2 px-5 py-2 text-sm bg-card border border-border rounded hover:bg-background transition',
        className,
      ].join(' ')}
      aria-label="Se connecter avec Google"
    >
      <img src="/google-logo.svg" alt="Google" className="w-5 h-5" />
      <span className="text-sm text-foreground">Se connecter avec Google</span>
    </button>
 );
}
