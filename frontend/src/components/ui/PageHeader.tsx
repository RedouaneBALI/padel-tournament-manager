'use client';

import React from 'react';
import BackButton from '@/src/components/ui/buttons/BackButton';

interface PageHeaderProps {
  title: React.ReactNode;
  showBackButton?: boolean;
  right?: React.ReactNode;
  loading?: boolean;
  className?: string;
}

export default function PageHeader({
  title,
  showBackButton = false,
  right,
  loading = false,
  className = '',
}: PageHeaderProps) {
  return (
    <div className="relative flex items-center justify-center mb-2 w-full">
      <BackButton className={`absolute left-0 top-1/2 -translate-y-1/2 ${showBackButton ? '' : 'invisible'}`} disabled={!showBackButton} />
      <h1 className="flex items-center gap-3 relative min-w-0">
        {loading ? (
          <span className="inline-block h-6 w-40 rounded bg-muted animate-pulse" />
        ) : (
          <span
            className="text-base font-semibold tracking-tight text-primary relative truncate overflow-hidden whitespace-nowrap block after:absolute after:bottom-0 after:left-0 after:w-full after:h-0.5 after:bg-gradient-to-r after:from-[#1b2d5e] after:to-white"
          >
            {title}
          </span>
        )}
      </h1>
      {right}
    </div>
  );
}