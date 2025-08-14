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
    default: 'Padel Tournament Manager',
    template: '%s · Padel Tournament Manager',
  },
  description: 'Crée, organise et gère des tournois de padel : tirages automatiques, tableaux knockout, suivi des scores en direct.',
  keywords: [
    'tournoi padel',
    'gestion tournoi padel',
    'tirage padel',
    'tableau knockout',
    'organisation tournoi',
    'padel manager',
  ],
  alternates: {
    canonical: '/',
  },
  openGraph: {
    type: 'website',
    url: '/',
    title: 'Padel Tournament Manager',
    description:
      'Crée, organise et gère des tournois de padel : tirages automatiques, tableaux knockout, suivi des scores en direct.',
    siteName: 'Padel Tournament Manager',
    images: [
      {
        url: '/og-cover.png',
        width: 1200,
        height: 630,
        alt: 'Padel Tournament Manager – Crée et gère tes tournois',
      },
    ],
    locale: 'fr_FR',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Padel Tournament Manager',
    description:
      'Crée, organise et gère des tournois de padel : tirages automatiques, tableaux knockout, suivi des scores en direct.',
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
          {session && (
            <header className="sticky top-0 z-40 bg-background/80 backdrop-blur border-b border-border">
              <div className="max-w-5xl mx-auto px-4">
                <nav className="h-14 flex items-center justify-center w-full">
                  <div className="flex items-center gap-10">
                    <Link href="/" className="flex items-center gap-2" aria-label="Accueil" title="Accueil">
                      <Image
                        src="/ptm-logo-cropped.png"
                        alt="Padel Tournament Manager"
                        width={32}
                        height={32}
                        priority
                        className="h-12 w-auto"
                      />
                      <span className="sr-only">Accueil</span>
                    </Link>

                    <Link
                      href="/admin/tournament/new"
                      className="flex items-center gap-2 text-muted hover:text-primary"
                      aria-label="Créer un tournoi"
                      title="Créer un tournoi"
                    >
                      <FiPlusCircle className="w-5 h-5" />
                      <span className="hidden md:inline">Créer un tournoi</span>
                    </Link>

                    <Link
                      href="/admin/tournaments"
                      className="flex items-center gap-2 text-muted hover:text-primary"
                      aria-label="Mes tournois"
                      title="Mes tournois"
                    >
                      <FiList className="w-5 h-5" />
                      <span className="hidden md:inline">Mes tournois</span>
                    </Link>

                    <Link
                      href="/contact"
                      className="flex items-center gap-2 text-muted hover:text-primary"
                      aria-label="Contact"
                      title="Contact"
                    >
                      <FiMail className="w-5 h-5" />
                      <span className="hidden md:inline">Contact</span>
                    </Link>

                    <LogoutButton>
                      <span className="hidden md:inline">Déconnexion</span>
                    </LogoutButton>
                  </div>
                </nav>
              </div>
            </header>
          )}
          {children}
        </SessionProviderWrapper>
      </body>
    </html>
  );
}