// app/layout.tsx

import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import 'react-confirm-alert/src/react-confirm-alert.css';
import SessionProviderWrapper from './SessionProviderWrapper';
import LogoutButton from '@/src/components/auth/LogoutButton';
import { getServerSession } from "next-auth";
import { getAuthOptions } from "@/src/lib/authOptions";
import Link from 'next/link';
import Image from 'next/image';
import { FiPlusCircle, FiList, FiMail } from 'react-icons/fi';

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL || 'http://localhost:3000'),
  title: {
    default: 'Padel Rounds',
    template: '%s · Padel Rounds',
  },
  description: 'Crée, organise et gère des tournois de padel : liste des joueurs, tableaux knockout, suivi des scores en direct.',
  keywords: [
    'tournoi padel',
    'gestion tournoi padel',
    'organiser tournoi padel',
    'logiciel gestion tournoi padel',
    'tirage padel',
    'tableau knockout',
    'tableau tournoi padel',
    'suivi scores padel',
    'scores en direct padel',
    'résultats match padel',
    'inscriptions tournoi padel',
    'joueurs padel',
    'bracket padel',
    'seed padel',
    'padel manager',
    'padel tournament',
    'tournament manager',
    'live scoring padel',
    'padel maroc',
    'padel casablanca',
    'tournoi padel maroc'
  ],
  icons: {
      icon: [
        { url: '/favicon.ico' },            // fallback
        { url: '/favicon.svg', type: 'image/svg+xml' },
        { url: '/pr-logo.png', type: 'image/png' },
      ],
      apple: [
        { url: '/apple-touch-icon.png', sizes: '180x180' },
      ],
      shortcut: ['/favicon.ico', '/pr-logo.png'],
      other: [
        { rel: 'mask-icon', url: '/mask-icon.svg', color: '#0ea5e9' }, // couleur du pin Safari
        { rel: 'manifest', url: '/site.webmanifest' },
      ],
    },
  alternates: {
    canonical: '/',
  },
  openGraph: {
    type: 'website',
    url: '/',
    title: 'Padel Rounds',
    description:
      'Crée, organise et gère des tournois de padel : liste des joueurs, tableaux knockout, suivi des scores en direct.',
    siteName: 'Padel Rounds',
    images: [
      {
        url: '/og-cover.png',
        width: 1200,
        height: 630,
        alt: 'Padel Rounds – Crée et gère tes tournois',
      },
    ],
    locale: 'fr_FR',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Padel Rounds',
    description:
      'Crée, organise et gère des tournois de padel : liste des joueurs, tableaux knockout, suivi des scores en direct.',
    images: ['/og-cover.png'],
  },
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

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(getAuthOptions());
  return (
    <html lang="fr" dir="ltr">
      <body className={`${geistSans.variable} ${geistMono.variable}`}>
        <SessionProviderWrapper>
          <header className="sticky top-0 z-[80] bg-background/80 border-b border-border">
            <style>{`summary::-webkit-details-marker, summary::marker{display:none;}`}</style>
            <div className="max-w-5xl mx-auto px-4">
              <nav className="h-14 flex items-center justify-between w-full">
                {/* Center: logo */}
                <Link href="/" className="flex items-center gap-2" aria-label="Accueil" title="Accueil">
                  <Image
                    src="/pr-logo.png"
                    alt="Padel Rounds"
                    width={32}
                    height={32}
                    priority
                    className="h-12 w-auto"
                  />
                  <span className="sr-only">Accueil</span>
                </Link>
              </nav>
            </div>
          </header>
          {children}
        </SessionProviderWrapper>
      </body>
    </html>
  );
}