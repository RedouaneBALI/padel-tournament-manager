'use client';

import React from 'react';

type Props = {
  title: React.ReactNode;
  right?: React.ReactNode;   // actions on the right (buttons, etc.)
  loading?: boolean;         // show a skeleton when data is not ready
  className?: string;
};

export default function PageHeader({ title, right, loading = false, className = '' }: Props) {
  return (
    <div className={`flex items-center justify-between mb-4 ${className}`}>
      <h1 className="flex items-center gap-3">
        <div className="w-1 h-10 bg-primary rounded" />
        {loading ? (
          <span className="inline-block h-6 w-40 rounded bg-muted animate-pulse" />
        ) : (
          <span className="text-2xl font-bold tracking-tight text-primary">{title}</span>
        )}
      </h1>
      {right ? <div className="flex items-center gap-2">{right}</div> : null}
    </div>
  );
}