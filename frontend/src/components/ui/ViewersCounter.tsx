'use client'

import React, { useEffect, useRef, useState } from 'react'

interface Props {
  gameId: string
  className?: string
}

export default function ViewersCounter({ gameId, className }: Props) {
  const [count, setCount] = useState<number | null>(null)
  const esRef = useRef<EventSource | null>(null)

  useEffect(() => {
    if (!gameId) return

    // close previous
    if (esRef.current) {
      try {
        esRef.current.close()
      } catch (e) {
        // ignore
      }
      esRef.current = null
    }

    const url = `/api/viewers/${encodeURIComponent(gameId)}`

    // EventSource automatically reconnects; keep a ref to close on unmount
    const es = new EventSource(url)
    esRef.current = es

    es.onmessage = (e) => {
      const text = String(e.data || '').trim()
      if (!text) return
      const num = parseInt(text, 10)
      if (!Number.isNaN(num)) setCount(num)
    }

    es.onerror = (err) => {
      // When an error occurs EventSource will try to reconnect automatically.
      // If the connection is closed permanently, setCount to null to indicate unknown.
      // We don't close here to allow builtin reconnection.
      // console.debug('ViewersCounter SSE error', err)
    }

    return () => {
      try {
        es.close()
      } catch (e) {
        // ignore
      }
      esRef.current = null
    }
  }, [gameId])

  return (
    <div
      aria-live="polite"
      aria-atomic="true"
      className={
        className ??
        'fixed bottom-26 left-1/2 -translate-x-1/2 bg-white/80 dark:bg-black/60 text-sm px-3 py-1 rounded-md shadow-md flex items-center gap-2 z-[80]'
      }
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 24 24"
        width="16"
        height="16"
        className="opacity-80"
        aria-hidden
      >
        <path
          fill="currentColor"
          d="M12 5c-7 0-11 7-11 7s4 7 11 7 11-7 11-7-4-7-11-7zm0 11a4 4 0 110-8 4 4 0 010 8z"
        />
        <circle cx="12" cy="12" r="2" fill="currentColor" />
      </svg>

      <span className="font-medium">Spectateurs</span>
      <span className="ml-2">{count ?? '...'}</span>
    </div>
  )
}
