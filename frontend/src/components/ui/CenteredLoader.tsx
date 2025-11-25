'use client';

import { Loader2 } from 'lucide-react';

interface CenteredLoaderProps {
  size?: number;
  className?: string;
}

export default function CenteredLoader({ size, className = '' }: CenteredLoaderProps) {
  const style = size ? { width: size, height: size } : undefined;
  return (
    <div className={`flex items-center justify-center py-8 text-muted-foreground ${className}`}>
      <Loader2
        className={`animate-spin ${size ? '' : 'loader-size'}`}
        style={style}
      />
    </div>
  );
}