// components/filters/AgeFilter.tsx

'use client'

import { useState, useEffect } from 'react'

type Props = {
  min: number | null
  max: number | null
  onChange: (range: { min: number | null; max: number | null }) => void
}

export default function AgeFilter({ min, max, onChange }: Props) {
  const [localMin, setLocalMin] = useState(min ?? '')
  const [localMax, setLocalMax] = useState(max ?? '')

  useEffect(() => {
    setLocalMin(min ?? '')
    setLocalMax(max ?? '')
  }, [min, max])

  useEffect(() => {
    const parsedMin = localMin === '' ? null : parseInt(String(localMin), 10)
    const parsedMax = localMax === '' ? null : parseInt(String(localMax), 10)
    onChange({ min: parsedMin, max: parsedMax })
  }, [localMin, localMax])

  const isAllSelected = min !== null && max !== null && localMin === String(min) && localMax === String(max)

  return (
    <div>
      <h3 className="text-base font-semibold text-gray-800 mb-2">Âge</h3>
      <div className="flex items-center gap-2">
        <input
          type="number"
          placeholder="Min"
          value={localMin}
          onChange={(e) => setLocalMin(e.target.value)}
          className="w-24 p-2 border border-gray-300 rounded"
        />
        <span>—</span>
        <input
          type="number"
          placeholder="Max"
          value={localMax}
          onChange={(e) => setLocalMax(e.target.value)}
          className="w-24 p-2 border border-gray-300 rounded"
        />
      </div>
    </div>
  )
}