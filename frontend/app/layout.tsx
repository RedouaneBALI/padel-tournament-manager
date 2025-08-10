import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import 'react-confirm-alert/src/react-confirm-alert.css';
import SessionProviderWrapper from './SessionProviderWrapper';
import LogoutButton from '@/src/components/auth/LogoutButton';
import { getServerSession } from "next-auth";
import { getAuthOptions } from "@/src/lib/authOptions";
import Link from 'next/link';
import { FiHome } from 'react-icons/fi';

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
            <div className="w-full flex justify-between p-4">
              <Link
                href="/"
                className="flex items-center text-muted hover:text-primary"
              >
                <FiHome className="w-6 h-6" />
              </Link>
              <LogoutButton />
            </div>
          )}
          {children}
        </SessionProviderWrapper>
      </body>
    </html>
  );
}