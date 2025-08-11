'use client';
import React from 'react';
import { ArrowDownTrayIcon } from '@heroicons/react/24/solid';

export default function BracketHeader({ onExport }: { onExport: () => void }) {
  return (
    <div className="flex items-center justify-between mb-4">
      <h2 className="text-xl font-semibold text-foreground">Arbre du tournoi</h2>
      <button
        onClick={onExport}
        className="p-2 text-on-primary bg-primary hover:bg-primary-hover rounded-md"
        title="Exporter en PNG"
      >
        <ArrowDownTrayIcon className="h-5 w-5" />
      </button>
    </div>
  );
}