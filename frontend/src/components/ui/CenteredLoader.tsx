'use client';

import { Loader2 } from 'lucide-react';

interface CenteredLoaderProps {
  size?: number;
  className?: string;
}

export default function CenteredLoader({ size = 24, className = '' }: CenteredLoaderProps) {
  return (
    <div className={`flex items-center justify-center py-8 text-muted-foreground ${className}`}>
      <Loader2
        className="animate-spin"
        style={{ width: size, height: size }}
      />
    </div>
  );
}