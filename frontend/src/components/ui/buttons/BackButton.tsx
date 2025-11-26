'use client';

import { useRouter } from 'next/navigation';
import { FiArrowLeft } from 'react-icons/fi';
import SecondaryButton from '@/src/components/ui/buttons/SecondaryButton';

interface Props {
  className?: string;
  label?: string;
}

export default function BackButton({ className = '', label = 'Retour' }: Props) {
  const router = useRouter();
  return (
    <SecondaryButton onClick={() => router.back()} className={className} ariaLabel={label}>
      <FiArrowLeft className="h-4 w-4" aria-hidden />
      <span>{label}</span>
    </SecondaryButton>
  );
}
