'use client';
import { SessionProvider } from 'next-auth/react';
import { ReactNode } from 'react';

export default function SessionProviderWrapper({ children }: { children: ReactNode }) {
  return (
    <SessionProvider
      refetchOnWindowFocus
      refetchWhenOffline={false}
      refetchInterval={5 * 60}   // each 5min
      refetchWhenHidden={false}
    >
      {children}
    </SessionProvider>
  );
}