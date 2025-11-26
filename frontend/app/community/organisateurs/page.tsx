'use client'

import React, { useEffect, useRef } from 'react'
import { INSTAGRAM_EMBEDS } from '@/src/data/instagramPosts'

export default function OrganisateursInstagramPage() {
  const containerRef = useRef<HTMLDivElement | null>(null)

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
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-3xl font-bold mb-4">Ils parlent de nous</h1>
      <p className="text-gray-600 mb-6">Une sélection de publications d'organisateurs qui utilisent PadelRounds</p>

      <div ref={containerRef} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {INSTAGRAM_EMBEDS.map((html, idx) => (
          <div key={idx} className="bg-card border border-border rounded-md p-4">
            <div dangerouslySetInnerHTML={{ __html: html }} />
          </div>
        ))}
      </div>
    </div>
  )
}

