'use client';

import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { fetchUserProfile } from '@/src/api/tournamentApi';

export default function CheckProfilePage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  useEffect(() => {

    if (status === 'loading') {
      return;
    }

    // Get returnUrl from localStorage
    const callbackUrl = typeof window !== 'undefined' ? localStorage.getItem('authReturnUrl') : null;
    const finalUrl = callbackUrl || '/admin/tournaments';


    if (!session) {
      router.push(`/connexion?returnUrl=${encodeURIComponent(finalUrl)}`);
      return;
    }


    const checkProfile = async () => {
      try {
        const user = await fetchUserProfile();

        if (!user.profileType) {
          router.push(`/mon-compte?returnUrl=${encodeURIComponent(finalUrl)}`);
        } else {
          // Clean up localStorage and redirect
          if (typeof window !== 'undefined') {
            localStorage.removeItem('authReturnUrl');
          }
          router.push(finalUrl);
        }
      } catch (error: any) {
        router.push(`/mon-compte?returnUrl=${encodeURIComponent(finalUrl)}`);
      }
    };

    checkProfile();
  }, [session, status, router]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto"></div>
        <p className="mt-4 text-gray-600">Vérification de votre profil...</p>
      </div>
    </div>
  );
}

