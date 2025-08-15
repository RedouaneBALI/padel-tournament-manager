'use client';

import Link from 'next/link';
import { useMemo } from 'react';

export type BottomNavItem = {
  href: string;
  label: string;
  Icon: React.ComponentType<{ className?: string }>;
  isActive?: (pathname: string) => boolean;
};

interface BottomNavProps {
  items: BottomNavItem[];
  pathname: string;
  className?: string;
  fixed?: boolean; // default true
}

export default function BottomNav({ items, pathname, className, fixed = true }: BottomNavProps) {
  const colsClass = useMemo(() => {
    const n = Math.max(1, Math.min(items.length, 6));
    return `grid-cols-${n}` as const;
  }, [items.length]);

  const computeActive = (href: string, isActive?: (p: string) => boolean) => {
    if (isActive) return isActive(pathname);
    if (pathname === href) return true;
    // Only treat prefix as active if no more specific item also matches
    if (pathname.startsWith(href + '/')) {
      const moreSpecific = items.some(it => it.href !== href && (pathname === it.href || pathname.startsWith(it.href + '/')));
      return !moreSpecific;
    }
    return false;
  };

  return (
    <nav
      className={[
        fixed ? 'fixed bottom-0 inset-x-0' : '',
        'border-t border-border bg-background z-50',
        'grid grid-flow-col',
        colsClass,
        className ?? '',
      ].join(' ').trim()}
      role="navigation"
      aria-label="Navigation principale"
    >
      {items.map(({ href, label, Icon, isActive }, idx) => {
        const active = computeActive(href, isActive);
        return (
          <Link
            key={idx}
            href={href}
            className={`relative flex flex-col items-center justify-center py-2 text-xs ${
              active ? 'text-primary' : 'text-muted-foreground hover:text-primary'
            }`}
            aria-current={active ? 'page' : undefined}
          >
            <Icon className="h-5 w-5" />
            <span className="leading-none mt-2">{label}</span>
            {active && <span className="absolute bottom-1 w-3/4 h-0.5 bg-primary rounded-full" />}
          </Link>
        );
      })}
    </nav>
  );
}
