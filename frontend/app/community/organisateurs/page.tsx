'use client'

import React, { useEffect, useRef } from 'react'
import { INSTAGRAM_EMBEDS } from '@/src/data/instagramPosts'
import BackButton from '@/src/components/ui/buttons/BackButton'
import { usePathname } from 'next/navigation';
import BottomNav, { BottomNavItem } from '@/src/components/ui/BottomNav';
import { ExternalLink } from 'lucide-react';
import Link from 'next/link';

export default function OrganisateursInstagramPage() {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const pathname = usePathname() ?? '';
  const items: BottomNavItem[] = [
    { href: '/', label: 'Accueil', Icon: require('lucide-react').Home, isActive: (p: string) => p === '/' },
    { href: '#more', label: 'Plus', Icon: require('react-icons/fi').FiMoreHorizontal },
  ];

  useEffect(() => {
    // inject the instagram embed script if not present
    if (typeof window !== 'undefined') {
      const win = window as any
      if (!win.instgrm) {
        const s = document.createElement('script')
        s.async = true
        s.src = '//www.instagram.com/embed.js'
        document.body.appendChild(s)
        s.onload = () => {
          try {
            win.instgrm?.Embeds?.process()
          } catch (e) {
            // ignore
          }
        }
      } else {
        try {
          win.instgrm?.Embeds?.process()
        } catch (e) {
          // ignore
        }
      }
    }
  }, [])

  return (
    <>
      <main className="min-h-screen bg-background">
        <div className="container mx-auto py-6 px-4 pb-24">
          <div className="mb-4 space-y-3">
            <div className="flex items-center gap-4">
              <BackButton />
              <div>
                <h1 className="text-4xl sm:text-5xl font-bold tracking-tight">Ils parlent de nous</h1>
                <p className="text-lg text-muted-foreground max-w-2xl">Nos derniers tournois</p>
              </div>
            </div>
          </div>

          <div ref={containerRef} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {INSTAGRAM_EMBEDS.map(({ title, club, tournamentId, html }, idx) => (
              <div key={idx} className="group bg-card border border-border rounded-xl overflow-hidden shadow-sm hover:shadow-md hover:border-primary/50 transition-all duration-300 hover:scale-105">
                <div className="p-5 bg-gradient-to-br from-card to-card/80 border-b border-border/50">
                  <div className="flex items-center justify-between mb-1">
                    <h2 className="text-lg font-bold text-foreground group-hover:text-primary transition-colors">{title}</h2>
                    <Link href={`/tournament/${tournamentId}/bracket`} className="text-primary hover:text-primary-hover transition-colors flex items-center gap-1">
                      Voir le tableau <ExternalLink className="h-4 w-4" />
                    </Link>
                  </div>
                  <p className="text-sm text-muted-foreground">{club}</p>
                </div>
                <div className="p-4">
                  <div dangerouslySetInnerHTML={{ __html: html }} />
                </div>
              </div>
            ))}
          </div>
        </div>
      </main>
      <BottomNav items={items} pathname={pathname} />
    </>
  )
}
