'use client';

import React, { createContext, useContext, useState, useCallback } from 'react';

interface ExportContextType {
  onExport: (() => void) | null;
  setExportFunction: (fn: (() => void) | null) => void;
}

const ExportContext = createContext<ExportContextType | undefined>(undefined);

export function ExportProvider({ children }: { children: React.ReactNode }) {
  const [exportFn, setExportFn] = useState<(() => void) | null>(null);

  const setExportFunction = useCallback((fn: (() => void) | null) => {
    setExportFn(() => fn);
  }, []);

  return (
    <ExportContext.Provider value={{ onExport: exportFn, setExportFunction }}>
      {children}
    </ExportContext.Provider>
  );
}

export function useExport() {
  const context = useContext(ExportContext);
  if (context === undefined) {
    throw new Error('useExport must be used within an ExportProvider');
  }
  return context;
}
