'use client';

import { usePathname } from 'next/navigation';
import HeaderContent from './HeaderContent';

interface ConditionalHeaderProps {
  children: React.ReactNode;
}

/**
 * Conditionally renders the header and applies margin to children.
 * In TV mode (/tv/*), header is hidden and no margin is applied.
 */
export default function ConditionalHeader({ children }: ConditionalHeaderProps) {
  const pathname = usePathname();
  const isTVMode = pathname?.startsWith('/tv/') ?? false;

  return (
    <>
      {!isTVMode && (
        <header className="sticky top-0 z-[80] bg-background/80 border-b border-border">
          <style>{`summary::-webkit-details-marker, summary::marker{display:none;}`}</style>
          <div className="max-w-5xl mx-auto px-2">
            <nav className="h-14 flex items-center justify-between w-full">
              <HeaderContent />
            </nav>
          </div>
        </header>
      )}
      <div style={{ marginTop: isTVMode ? '0' : '15px' }}>
        {children}
      </div>
    </>
  );
}

