import { useEffect, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { PlayerFilters } from '../PlayerDetailModal';

function serializeFilters(filters: PlayerFilters): URLSearchParams {
  const params = new URLSearchParams();

  if (filters.nationalities.length > 0) {
    params.set('nat', filters.nationalities.join(','));
  }

  if (filters.clubs.length > 0) {
    params.set('clubs', filters.clubs.join(','));
  }

  if (filters.rankingRange.min !== null) {
    params.set('rankMin', filters.rankingRange.min.toString());
  }
  if (filters.rankingRange.max !== null) {
    params.set('rankMax', filters.rankingRange.max.toString());
  }

  if (filters.pointsRange.min !== null) {
    params.set('ptsMin', filters.pointsRange.min.toString());
  }
  if (filters.pointsRange.max !== null) {
    params.set('ptsMax', filters.pointsRange.max.toString());
  }

  if (filters.ageRange.min !== null) {
    params.set('ageMin', filters.ageRange.min.toString());
  }
  if (filters.ageRange.max !== null) {
    params.set('ageMax', filters.ageRange.max.toString());
  }

  return params;
}

function deserializeFilters(searchParams: URLSearchParams): PlayerFilters {
  const filters: PlayerFilters = {
    nationalities: [],
    clubs: [],
    rankingRange: { min: null, max: null },
    pointsRange: { min: null, max: null },
    ageRange: { min: null, max: null },
  };

  const nat = searchParams.get('nat');
  if (nat) {
    filters.nationalities = nat.split(',').filter(Boolean);
  }

  const clubs = searchParams.get('clubs');
  if (clubs) {
    filters.clubs = clubs.split(',').filter(Boolean);
  }

  const rankMin = searchParams.get('rankMin');
  if (rankMin) {
    const parsed = parseInt(rankMin, 10);
    if (!isNaN(parsed)) filters.rankingRange.min = parsed;
  }

  const rankMax = searchParams.get('rankMax');
  if (rankMax) {
    const parsed = parseInt(rankMax, 10);
    if (!isNaN(parsed)) filters.rankingRange.max = parsed;
  }

  const ptsMin = searchParams.get('ptsMin');
  if (ptsMin) {
    const parsed = parseInt(ptsMin, 10);
    if (!isNaN(parsed)) filters.pointsRange.min = parsed;
  }

  const ptsMax = searchParams.get('ptsMax');
  if (ptsMax) {
    const parsed = parseInt(ptsMax, 10);
    if (!isNaN(parsed)) filters.pointsRange.max = parsed;
  }

  const ageMin = searchParams.get('ageMin');
  if (ageMin) {
    const parsed = parseInt(ageMin, 10);
    if (!isNaN(parsed)) filters.ageRange.min = parsed;
  }

  const ageMax = searchParams.get('ageMax');
  if (ageMax) {
    const parsed = parseInt(ageMax, 10);
    if (!isNaN(parsed)) filters.ageRange.max = parsed;
  }

  return filters;
}

export function useUrlFilters(initialFilters: PlayerFilters) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isInitialized, setIsInitialized] = useState(false);

  // Initialize filters from URL
  const urlFilters = deserializeFilters(searchParams);
  const hasUrlParams = searchParams.toString().length > 0;

  const [filters, setFilters] = useState<PlayerFilters>(
    hasUrlParams ? urlFilters : initialFilters
  );

  // Only run once on mount to check for URL params
  useEffect(() => {
    if (!isInitialized && hasUrlParams) {
      setFilters(urlFilters);
    }
    setIsInitialized(true);
  }, []);

  // Update URL when filters change
  const updateFilters = (newFilters: PlayerFilters) => {
    setFilters(newFilters);

    const params = serializeFilters(newFilters);
    const queryString = params.toString();

    // Update URL without reload
    if (queryString) {
      router.replace(`?${queryString}`, { scroll: false });
    } else {
      router.replace(window.location.pathname, { scroll: false });
    }
  };

  return {
    filters,
    setFilters: updateFilters,
  };
}

