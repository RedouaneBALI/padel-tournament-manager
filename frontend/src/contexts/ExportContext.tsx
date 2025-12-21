'use client';

import React, { createContext, useContext, useState, useCallback, useMemo } from 'react';

interface AdminHeaderContextType {
  onExport: (() => void) | null;
  onShare: (() => void) | null;
  onEdit: (() => void) | null;
  tvButtonUrl: string | null;
  showTvButton: boolean;
  isAdmin: boolean;
  tournamentName: string | null;
  setAdminActions: (actions: Partial<AdminHeaderContextType>) => void;
  setTournamentName: (name: string | null) => void;
}

const AdminHeaderContext = createContext<AdminHeaderContextType | undefined>(undefined);

export function ExportProvider({ children }: { children: React.ReactNode }) {
  const [adminActions, setAdminActionsState] = useState<Omit<AdminHeaderContextType, 'setAdminActions' | 'setTournamentName'>>({
    onExport: null,
    onShare: null,
    onEdit: null,
    tvButtonUrl: null,
    showTvButton: false,
    isAdmin: false,
    tournamentName: null,
  });

  const setAdminActions = useCallback((actions: Partial<AdminHeaderContextType>) => {
    setAdminActionsState((prev) => ({ ...prev, ...actions }));
  }, []);

  const setTournamentName = useCallback((name: string | null) => {
    setAdminActionsState((prev) => ({ ...prev, tournamentName: name }));
  }, []);

  const value = useMemo(
    () => ({ ...adminActions, setAdminActions, setTournamentName }),
    [adminActions, setAdminActions, setTournamentName]
  );

  return (
    <AdminHeaderContext.Provider value={value}>
      {children}
    </AdminHeaderContext.Provider>
  );
}

export function useExport() {
  const context = useContext(AdminHeaderContext);
  if (context === undefined) {
    throw new Error('useExport must be used within an ExportProvider');
  }
  return context;
}
