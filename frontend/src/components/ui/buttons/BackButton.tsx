'use client';

import { useRouter } from 'next/navigation';
import { FiArrowLeft } from 'react-icons/fi';

interface Props {
  className?: string;
  label?: string;
}

export default function BackButton({ className = '', label = 'Retour' }: Props) {
  const router = useRouter();
  return (
    <button
      type="button"
      onClick={() => router.back()}
      className={[
        'inline-flex items-center gap-2 px-3 py-2 rounded-md border border-border',
        'hover:bg-accent hover:text-accent-foreground text-sm',
        className,
      ].join(' ')}
      aria-label={label}
    >
      <FiArrowLeft className="h-4 w-4" aria-hidden />
      <span>{label}</span>
    </button>
  );
}
