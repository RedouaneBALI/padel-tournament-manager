// app/tournament/layout.tsx
import type { Metadata } from 'next';

// This Server Component layout generates metadata for all /tournament/* pages
// to ensure they are indexed by Google Search Console
export const metadata: Metadata = {
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-image-preview': 'large',
      'max-snippet': -1,
      'max-video-preview': -1,
    },
  },
};

export default function TournamentRootLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}

