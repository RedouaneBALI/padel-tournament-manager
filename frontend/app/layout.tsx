import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import 'react-confirm-alert/src/react-confirm-alert.css';
import SessionProviderWrapper from './SessionProviderWrapper';
import LogoutButton from '@/src/components/auth/LogoutButton';
import { getServerSession } from "next-auth";
import { getAuthOptions } from "@/src/lib/authOptions";
import Link from 'next/link';
import { FiHome, FiPlusCircle, FiList } from 'react-icons/fi';

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Padel Tournament Manager",
  description: "By Redouane Bali",
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(getAuthOptions());
  return (
    <html lang="fr">
      <body className={`${geistSans.variable} ${geistMono.variable}`}>
        <SessionProviderWrapper>
          {session && (
            <header className="sticky top-0 z-40 bg-background/80 backdrop-blur border-b border-border">
              <div className="max-w-5xl mx-auto px-4">
                {/* Tout au centre, espacement homogène */}
                <nav className="h-14 flex items-center justify-center">
                  <div className="flex items-center gap-6 sm:gap-8">
                    <Link
                      href="/"
                      className="flex items-center text-muted hover:text-primary"
                      aria-label="Accueil"
                      title="Accueil"
                    >
                      <FiHome className="w-6 h-6" />
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
                      href="/admin/tournaments"   // ✅ URL corrigée
                      className="flex items-center gap-2 text-muted hover:text-primary"
                      aria-label="Mes tournois"
                      title="Mes tournois"
                    >
                      <FiList className="w-5 h-5" />
                      <span className="hidden md:inline">Mes tournois</span>
                    </Link>

                    {/* On garde le composant logout mais on le place dans le groupe centré */}
                    <div className="flex items-center">
                      <LogoutButton />
                    </div>
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