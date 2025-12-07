'use client';

import { useSession } from 'next-auth/react';
import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { fetchUserProfile, updateUserProfile } from '@/src/api/tournamentApi';
import { User, ProfileType } from '@/src/types/user';
import Button from '@/src/components/ui/buttons/Button';
import BottomNav from '@/src/components/ui/BottomNav';
import { getDefaultBottomItems } from '@/src/components/ui/bottomNavPresets';
import { Eye, UserIcon, Trophy } from 'lucide-react';

export default function MonComptePage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const searchParams = useSearchParams();
  const pathname = usePathname() ?? '';
  const callbackUrl = (searchParams?.get('callbackUrl')) || '/admin/tournaments';

  const bottomItems = getDefaultBottomItems();

  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    profileType: ProfileType.SPECTATOR,
  });

  useEffect(() => {
    if (status === 'loading') return;

    if (!session) {
      router.push('/connexion');
      return;
    }

    const loadProfile = async () => {
      try {
        const profile = await fetchUserProfile();
        setUser(profile);
        setFormData({
          name: profile.name || '', // Utiliser le nom du profil existant, même s'il est vide
          profileType: profile.profileType || ProfileType.SPECTATOR,
        });
      } catch (error: any) {
        console.error('Error loading profile:', error);
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, [session, status, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const updatedUser = await updateUserProfile(formData);
      setUser(updatedUser);
      router.push(callbackUrl);
    } catch (error) {
      console.error('Error updating profile:', error);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
          <p className="mt-4 text-muted-foreground">Chargement de votre profil...</p>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="max-w-lg mx-auto mt-10 p-6 bg-card rounded-lg shadow-md">
        <h1 className="text-2xl font-bold mb-6 text-center text-card-foreground">Complétez votre profil</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-foreground">
              Nom
            </label>
            <input
              type="text"
              id="name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="mt-1 block w-full px-3 py-2 border border-border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-ring"
              placeholder="Votre nom"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-foreground mb-3">
              Type de profil
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div
                className={`p-4 border rounded-lg cursor-pointer transition ${
                  formData.profileType === ProfileType.SPECTATOR
                    ? 'border-primary bg-primary/10'
                    : 'border-border hover:border-primary'
                }`}
                onClick={() => setFormData({ ...formData, profileType: ProfileType.SPECTATOR })}
              >
                <Eye className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
                <h3 className="text-center font-bold">Spectateur</h3>
                <p className="text-center text-sm text-muted-foreground">Regardez les matchs</p>
              </div>
              <div
                className={`p-4 border rounded-lg cursor-pointer transition ${
                  formData.profileType === ProfileType.PLAYER
                    ? 'border-primary bg-primary/10'
                    : 'border-border hover:border-primary'
                }`}
                onClick={() => setFormData({ ...formData, profileType: ProfileType.PLAYER })}
              >
                <UserIcon className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
                <h3 className="text-center font-bold">Joueur</h3>
                <p className="text-center text-sm text-muted-foreground">Participez aux tournois</p>
              </div>
              <div
                className={`p-4 border rounded-lg cursor-pointer transition ${
                  formData.profileType === ProfileType.ORGANIZER
                    ? 'border-primary bg-primary/10'
                    : 'border-border hover:border-primary'
                }`}
                onClick={() => setFormData({ ...formData, profileType: ProfileType.ORGANIZER })}
              >
                <Trophy className="h-8 w-8 mx-auto mb-2 text-muted-foreground" />
                <h3 className="text-center font-bold">Organisateur</h3>
                <p className="text-center text-sm text-muted-foreground">Créez et gérez des tournois</p>
              </div>
            </div>
          </div>
          <Button
            type="submit"
            disabled={saving}
            className="w-full"
          >
            {saving ? 'Sauvegarde...' : 'Sauvegarder et continuer'}
          </Button>
        </form>
      </div>
      {user && <BottomNav items={bottomItems} pathname={pathname} />}
    </>
  );
}
