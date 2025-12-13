'use client';

import React, { createContext, useContext, useState, useCallback, useMemo } from 'react';

interface AdminHeaderContextType {
  onExport: (() => void) | null;
  onShare: (() => void) | null;
  onEdit: (() => void) | null;
  tvButtonUrl: string | null;
  showTvButton: boolean;
  isAdmin: boolean;
  setAdminActions: (actions: Partial<AdminHeaderContextType>) => void;
}

const AdminHeaderContext = createContext<AdminHeaderContextType | undefined>(undefined);

export function ExportProvider({ children }: { children: React.ReactNode }) {
  const [adminActions, setAdminActionsState] = useState<Omit<AdminHeaderContextType, 'setAdminActions'>>({
    onExport: null,
    onShare: null,
    onEdit: null,
    tvButtonUrl: null,
    showTvButton: false,
    isAdmin: false,
  });

  const setAdminActions = useCallback((actions: Partial<AdminHeaderContextType>) => {
    setAdminActionsState((prev) => ({ ...prev, ...actions }));
  }, []);

  const value = useMemo(
    () => ({ ...adminActions, setAdminActions }),
    [adminActions, setAdminActions]
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
