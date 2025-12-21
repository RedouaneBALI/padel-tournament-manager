import { useRouter } from 'next/navigation';
import { FiArrowLeft } from 'react-icons/fi';

export default function BackButton({ className = '', disabled = false }: { className?: string; disabled?: boolean }) {
  const router = useRouter();
  return (
    <button
      onClick={() => !disabled && router.back()}
      disabled={disabled}
      className={`p-4 rounded hover:bg-muted transition-colors cursor-pointer ${className}`}
      aria-label="Retour"
      type="button"
    >
      <FiArrowLeft className="h-5 w-5 text-muted-foreground hover:text-primary" aria-hidden />
    </button>
  );
}
