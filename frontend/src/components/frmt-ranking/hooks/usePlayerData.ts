// /hooks/usePlayerData.ts

import { useEffect, useState, useMemo } from 'react'
import { Player, PlayerFilters } from '../PlayerDetailModal'

export type PlayerSortableColumn = keyof Player;

function matchesSearch(player: Player, search: string): boolean {
  if (search.trim() === '') return true;
  const searchTerms = search.toLowerCase().split(' ').filter(term => term !== '');
  const playerName = player.name.toLowerCase();
  return searchTerms.every(term => playerName.includes(term));
}

function passesFilters(player: Player, activeFilters: PlayerFilters, currentYear: number): boolean {
  if (activeFilters.nationalities.length > 0 && !activeFilters.nationalities.includes(player.nationality)) {
    return false;
  }
  if (activeFilters.clubs.length > 0 && !activeFilters.clubs.includes(player.club)) {
    return false;
  }

  const { rankingRange, pointsRange, ageRange } = activeFilters;
  if (rankingRange.min !== null && player.ranking < rankingRange.min) {
    return false;
  }
  if (rankingRange.max !== null && player.ranking > rankingRange.max) {
    return false;
  }
  if (pointsRange.min !== null && player.points < pointsRange.min) {
    return false;
  }
  if (pointsRange.max !== null && player.points > pointsRange.max) {
    return false;
  }

  if (player.birth_year) {
    const playerAge = currentYear - player.birth_year;
    if (ageRange.min !== null && playerAge < ageRange.min) {
      return false;
    }
    if (ageRange.max !== null && playerAge > ageRange.max) {
      return false;
    }
  } else if (ageRange.min !== null || ageRange.max !== null) {
    return false;
  }

  return true;
}

function sortPlayers(filteredData: Player[], sortKey: PlayerSortableColumn | null, sortOrder: 'asc' | 'desc'): Player[] {
  if (!sortKey) return filteredData;

  return filteredData.sort((a, b) => {
    const aVal = a[sortKey];
    const bVal = b[sortKey];

    if (typeof aVal === 'number' && typeof bVal === 'number') {
      return sortOrder === 'asc' ? aVal - bVal : bVal - aVal;
    }
    if (sortKey === 'ranking' || sortKey === 'points' || sortKey === 'evolution' || sortKey === 'point_diff') {
      const numA = parseFloat(String(aVal)) || 0;
      const numB = parseFloat(String(bVal)) || 0;
      return sortOrder === 'asc' ? numA - numB : numB - numA;
    }
    return sortOrder === 'asc'
      ? String(aVal).localeCompare(String(bVal))
      : String(bVal).localeCompare(String(aVal));
  });
}

function processPlayers(allPlayersData: Player[], search: string, activeFilters: PlayerFilters, sortKey: PlayerSortableColumn | null, sortOrder: 'asc' | 'desc'): Player[] {
  const currentYear = new Date().getFullYear();

  const filteredData = allPlayersData.filter((player) => {
    return matchesSearch(player, search) && passesFilters(player, activeFilters, currentYear);
  });

  return sortPlayers(filteredData, sortKey, sortOrder);
}

export function usePlayerData(jsonUrl: string, pageSize = 100) {
  const [allPlayersData, setAllPlayersData] = useState<Player[]>([])
  const [scrapedAt, setScrapedAt] = useState<string | null>(null)

  const [search, setSearch] = useState('')
  const [activeFilters, setActiveFilters] = useState<PlayerFilters>({
    nationalities: [],
    clubs: [],
    rankingRange: { min: null, max: null },
    pointsRange: { min: null, max: null },
    ageRange: { min: null, max: null },
  })
  const [currentPage, setCurrentPage] = useState(1)
  const [sortKey, setSortKey] = useState<PlayerSortableColumn | null>('ranking')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc')

  const initialFiltersState: PlayerFilters = {
    nationalities: [],
    clubs: [],
    ageRange: { min: null, max: null },
    rankingRange: { min: null, max: null },
    pointsRange: { min: null, max: null },
  };

  useEffect(() => {
    async function fetchData() {
      try {
        const res = await fetch(`/${jsonUrl}`)
        const content = await res.json()
        console.log(" players data : " + content.data.length);
        setAllPlayersData(content.data || [])
        setScrapedAt(content.scraped_at || null)
      } catch (error) {
        console.error("Failed to fetch player data:", error)
        setAllPlayersData([])
      }
    }
    fetchData()
  }, [jsonUrl])

  const handleSort = (key: PlayerSortableColumn) => {
    if (sortKey === key) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')
    } else {
      setSortKey(key)
      setSortOrder('asc')
    }
    setCurrentPage(1)
  }

  const processedPlayers = useMemo(() => {
    return processPlayers(allPlayersData, search, activeFilters, sortKey, sortOrder);
  }, [allPlayersData, search, activeFilters, sortKey, sortOrder])

  const totalPages = Math.ceil(processedPlayers.length / pageSize)
  const start = (currentPage - 1) * pageSize
  const paginatedData = processedPlayers.slice(start, start + pageSize)

  return {
    data: paginatedData,
    allData: allPlayersData,
    initialFilters: initialFiltersState,
    totalPages,
    currentPage,
    setCurrentPage,
    totalResults: processedPlayers.length,
    search,
    setSearch,
    sortKey,
    sortOrder,
    handleSort,
    scrapedAt,
    activeFilters,
    setActiveFilters
  }
}