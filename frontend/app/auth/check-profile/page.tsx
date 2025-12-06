'use client';

import { useSession } from 'next-auth/react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useEffect } from 'react';
import { fetchUserProfile } from '@/src/api/tournamentApi';

export default function CheckProfilePage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const searchParams = useSearchParams();
  const callbackUrl = (searchParams?.get('callbackUrl')) || '/admin/tournaments';

  useEffect(() => {
    if (status === 'loading') return;

    if (!session) {
      router.push('/connexion');
      return;
    }

    const checkProfile = async () => {
      try {
        const user = await fetchUserProfile();
        console.log('User profile fetched:', user);

        // Si le profil existe mais que le nom n'est pas défini, considérer comme profil incomplet
        if (!user.name || user.name.trim() === '') {
          console.log("cas 1");
          router.push('/mon-compte');
        } else {
          console.log("cas 2");
          router.push(callbackUrl);
        }
      } catch (error: any) {
        console.log('Profile check error:', error.message);
        // Profil inexistant ou autre erreur - rediriger vers mon-compte
        router.push('/mon-compte');
      }
    };

    checkProfile();
  }, [session, status, router, callbackUrl]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto"></div>
        <p className="mt-4 text-gray-600">Vérification de votre profil...</p>
      </div>
    </div>
  );
}
