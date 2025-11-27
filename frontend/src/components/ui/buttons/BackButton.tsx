'use client';

import { useRouter } from 'next/navigation';
import { FiArrowLeft } from 'react-icons/fi';
import Button from '@/src/components/ui/buttons/Button';

interface Props {
  className?: string;
  label?: string;
}

export default function BackButton({ className = '', label = 'Retour' }: Props) {
  const router = useRouter();
  return (
    <Button onClick={() => router.back()} className={className} variant="secondary" aria-label={label}>
      <FiArrowLeft className="h-4 w-4" aria-hidden />
      <span>{label}</span>
    </Button>
  );
}
