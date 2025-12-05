'use client'

import { useState, useMemo } from 'react'
import { ChevronDown, ChevronUp } from 'lucide-react'
import { getFlagEmoji } from '../utils/flags'
import MultiSelectFilter from '../filters/MultiSelectFilter'

interface Props {
  availableClubs: string[];
  selectedClubs: string[];
  onChange: (newSelection: string[]) => void;
  allPlayers?: { club: string }[];
}

export default function ClubFilter({
  availableClubs,
  selectedClubs,
  onChange,
  allPlayers = [],
}: Props) {
  const [open, setOpen] = useState(false);

  const toggleClub = (club: string) => {
    const newSelection = selectedClubs.includes(club)
      ? selectedClubs.filter(c => c !== club)
      : [...selectedClubs, club];
    onChange(newSelection);
  };

  const handleToggleAll = () => {
    if (selectedClubs.length === availableClubs.length) {
      onChange([]);
    } else {
      onChange(availableClubs);
    }
  };

  const clubCounts = useMemo(() => {
    return allPlayers.reduce<Record<string, number>>((acc, player) => {
      const club = player.club;
      if (!club) return acc;
      acc[club] = (acc[club] || 0) + 1;
      return acc;
    }, {});
  }, [allPlayers]);

  const isAllSelected = availableClubs && selectedClubs && availableClubs.length > 0 && selectedClubs.length === availableClubs.length;

  return (
    <MultiSelectFilter
      label="Club"
      availableOptions={availableClubs}
      selectedOptions={selectedClubs}
      onChange={onChange}
      allItems={allPlayers}
      keyField="club"
    />
  );
}