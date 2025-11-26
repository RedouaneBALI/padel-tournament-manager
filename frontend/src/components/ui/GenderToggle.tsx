'use client'

import React, { useCallback, useRef } from 'react'

interface Props {
  value: 'M' | 'F'
  onChange: (v: 'M' | 'F') => void
  className?: string
}

export default function GenderToggle({ value, onChange, className = '' }: Props) {
  const btnMRef = useRef<HTMLButtonElement | null>(null)
  const btnFRef = useRef<HTMLButtonElement | null>(null)

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
      e.preventDefault()
      onChange('M')
      btnMRef.current?.focus()
    } else if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
      e.preventDefault()
      onChange('F')
      btnFRef.current?.focus()
    }
  }, [onChange])

  return (
    <div className={['inline-flex items-center gap-2 bg-card p-1 rounded-full', className].join(' ')} role="radiogroup" aria-label="Genre">
      <button
        ref={btnMRef}
        role="radio"
        aria-checked={value === 'M'}
        tabIndex={value === 'M' ? 0 : -1}
        onKeyDown={handleKeyDown}
        onClick={() => onChange('M')}
        className={[
          'px-4 py-2 rounded-full text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
          value === 'M' ? 'bg-primary text-on-primary' : 'bg-transparent text-foreground hover:bg-primary/10'
        ].join(' ')}
      >
        <span aria-hidden className="mr-2">♂</span>
        <span>Homme</span>
      </button>

      <button
        ref={btnFRef}
        role="radio"
        aria-checked={value === 'F'}
        tabIndex={value === 'F' ? 0 : -1}
        onKeyDown={handleKeyDown}
        onClick={() => onChange('F')}
        className={[
          'px-4 py-2 rounded-full text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
          value === 'F' ? 'bg-primary text-on-primary' : 'bg-transparent text-foreground hover:bg-primary/10'
        ].join(' ')}
      >
        <span aria-hidden className="mr-2">♀</span>
        <span>Femme</span>
      </button>
    </div>
  )
}

