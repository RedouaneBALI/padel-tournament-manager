'use client'

import { useState, useMemo } from 'react'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { getFlagEmoji } from '../utils/flags'
import MultiSelectFilter from '../filters/MultiSelectFilter'

interface Props {
  availableNationalities: string[]
  selectedNationalities: string[]
  onChange: (newSelection: string[]) => void
  allPlayers?: { nationality: string }[]
}

export default function NationalityFilter({
  availableNationalities,
  selectedNationalities,
  onChange,
  allPlayers = []
}: Props) {
  const [open, setOpen] = useState(false)

  const toggleNationality = (nat: string) => {
    const newSelection = selectedNationalities.includes(nat)
      ? selectedNationalities.filter(n => n !== nat)
      : [...selectedNationalities, nat]
    onChange(newSelection)
  }

  const handleToggleAll = () => {
    if (selectedNationalities.length === availableNationalities.length) {
      onChange([])
    } else {
      onChange(availableNationalities)
    }
  }

  const nationalityCounts = useMemo(() => {
    return allPlayers.reduce<Record<string, number>>((acc, player) => {
      const nat = player.nationality
      if (!nat) return acc
      acc[nat] = (acc[nat] || 0) + 1
      return acc
    }, {})
  }, [allPlayers])

  const isAllSelected = availableNationalities.length > 0 && selectedNationalities.length === availableNationalities.length

  return (
    <MultiSelectFilter
      label="Nationalité"
      availableOptions={availableNationalities}
      selectedOptions={selectedNationalities}
      onChange={onChange}
      allItems={allPlayers}
      keyField="nationality"
      renderPrefix={(nat) => getFlagEmoji(nat)}
    />
  )
}