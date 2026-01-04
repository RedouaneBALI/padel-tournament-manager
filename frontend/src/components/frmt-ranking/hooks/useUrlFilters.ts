import { useEffect, useState } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { PlayerFilters } from '../PlayerDetailModal';

type ArrayFilterKey = 'nat' | 'clubs';
type RangeFilterKey = 'rankMin' | 'rankMax' | 'ptsMin' | 'ptsMax' | 'ageMin' | 'ageMax';

const ARRAY_FILTERS: Record<ArrayFilterKey, keyof PlayerFilters> = {
  nat: 'nationalities',
  clubs: 'clubs',
};

const RANGE_FILTERS: Record<RangeFilterKey, { range: keyof PlayerFilters; field: 'min' | 'max' }> = {
  rankMin: { range: 'rankingRange', field: 'min' },
  rankMax: { range: 'rankingRange', field: 'max' },
  ptsMin: { range: 'pointsRange', field: 'min' },
  ptsMax: { range: 'pointsRange', field: 'max' },
  ageMin: { range: 'ageRange', field: 'min' },
  ageMax: { range: 'ageRange', field: 'max' },
};

function parseIntSafe(value: string | null): number | null {
  if (!value) return null;
  const parsed = Number.parseInt(value, 10);
  return Number.isNaN(parsed) ? null : parsed;
}

function serializeFilters(filters: PlayerFilters): URLSearchParams {
  const params = new URLSearchParams();

  (Object.entries(ARRAY_FILTERS) as [ArrayFilterKey, keyof PlayerFilters][]).forEach(
    ([key, filterKey]) => {
      const values = filters[filterKey] as string[];
      if (values.length > 0) {
        params.set(key, values.join(','));
      }
    }
  );

  (Object.entries(RANGE_FILTERS) as [RangeFilterKey, { range: keyof PlayerFilters; field: 'min' | 'max' }][]).forEach(
    ([key, { range, field }]) => {
      const value = (filters[range] as Record<string, number | null>)[field];
      if (value !== null) {
        params.set(key, value.toString());
      }
    }
  );

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

  (Object.entries(ARRAY_FILTERS) as [ArrayFilterKey, keyof PlayerFilters][]).forEach(
    ([key, filterKey]) => {
      const value = searchParams.get(key);
      if (value) {
        (filters[filterKey] as string[]) = value.split(',').filter(Boolean);
      }
    }
  );

  (Object.entries(RANGE_FILTERS) as [RangeFilterKey, { range: keyof PlayerFilters; field: 'min' | 'max' }][]).forEach(
    ([key, { range, field }]) => {
      const parsed = parseIntSafe(searchParams.get(key));
      if (parsed !== null) {
        ((filters[range] as Record<string, number | null>)[field]) = parsed;
      }
    }
  );

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

