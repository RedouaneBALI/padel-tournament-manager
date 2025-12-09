// app/page.tsx
'use client';

import Link from 'next/link';
import Image from 'next/image';
import { signIn, signOut, useSession } from 'next-auth/react';
import { usePathname } from 'next/navigation';
import ContactButton from '@/src/components/ui/buttons/ContactButton';
import CommunityButton from '@/src/components/ui/buttons/CommunityButton';
import BottomNav from '@/src/components/ui/BottomNav';
import { getDefaultBottomItems } from '@/src/components/ui/bottomNavPresets';
import ImageSlider from '@/src/components/ui/ImageSlider';
import { useEffect, useState } from 'react';
import FeaturedTournaments from '@/src/components/home/FeaturedTournaments';
import { fetchActiveTournaments } from '@/src/api/tournamentApi';
import { formatDate, formatDateRange, levelEmoji, genderEmoji, genderLabel, filterActiveTournaments } from '@/src/components/home/tournamentHelpers';

export default function Home() {
  const { status } = useSession();

  const pathname = usePathname() ?? '';
  const bottomItems = getDefaultBottomItems();

  const [featured, setFeatured] = useState<any[]>([]);
  const [loadingFeatured, setLoadingFeatured] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoadingFeatured(true);
    fetchActiveTournaments()
      .then((list) => {
        console.log('Fetched list from API:', list);
        if (mounted) setFeatured(filterActiveTournaments(list));
      })
      .catch(() => {})
      .finally(() => mounted && setLoadingFeatured(false));
    return () => {
      mounted = false;
    };
  }, []);

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'WebSite',
    name: 'Padel Rounds',
    url: process.env.NEXT_PUBLIC_SITE_URL || 'http://localhost:3000/',
    potentialAction: {
      '@type': 'SearchAction',
      target: {
        '@type': 'EntryPoint',
        urlTemplate: (process.env.NEXT_PUBLIC_SITE_URL || 'http://localhost:3000') + '/admin/tournament/new?q={search_term_string}',
      },
      'query-input': 'required name=search_term_string',
    },
  } as const;

  const features = [
    {
      title: "Création rapide",
      description: "Renseigne quelques infos et démarre en quelques minutes.",
      emoji: "⚡"
    },
    {
      title: "Paires & tirages",
      description: "Ajoute les équipes, configure les formats, génère le tirage.",
      emoji: "👥"
    },
    {
      title: "Gestion en direct",
      description: "Mets à jour les résultats et partage l'avancement du tournoi en temps réel.",
      emoji: "⏱️"
    }
  ];
  // Authenticated: show the main hero with action buttons
  return (
    <main className="min-h-screen bg-background">
      <section className="max-w-5xl mx-auto px-4 py-4 sm:py-8">
        <div className="bg-card border border-border rounded-2xl p-8 sm:p-12 shadow-sm">
          <div className="text-center space-y-6 mb-6">
            <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />

            {!loadingFeatured && <FeaturedTournaments items={featured} loading={false} />}

            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight mb-2">
              VIS TES TOURNOIS INTENSÉMENT <span role="img" aria-label="flamme">🔥</span>
            </h1>

            <p className="text-lg font-semibold mb-2">
              Une seule app pour </p>
            <p className="text-lg font-semibold mb-2">
              Organiser · Suivre · Partager · Jouer l'esprit tranquille
            </p>

            <div className="flex justify-center">
              <Image
                src="/pr-logo.png"
                alt="Padel Rounds"
                className="h-40 w-auto"
                width={1300}
                height={1300}
                priority
              />
            </div>

            <h2 className="text-2xl sm:text-3xl font-bold text-center my-6">
              Joue. On s'occupe du reste. <span role="img" aria-label="balle de tennis">🎾</span>
            </h2>

            <div className="flex flex-wrap justify-center gap-3 sm:gap-4">
              {status === 'authenticated' ? (
                <div className="flex w-full max-w-md mx-auto gap-3">
                  <Link
                    href="/admin/tournament/new"
                    className="flex-1 text-center px-5 py-3 rounded-lg bg-primary text-on-primary font-semibold hover:bg-primary-hover transition"
                  >
                    Créer un tournoi 🎾
                  </Link>
                  <Link
                    href="/frmt/classement/hommes"
                    className="flex-1 text-center px-5 py-3 rounded-lg bg-primary text-on-primary font-semibold hover:bg-primary-hover transition"
                  >
                    Classement FRMT 🔎
                  </Link>
                </div>
              ) : (
                <>
                  <ImageSlider />
                  <div className="flex flex-col items-center w-full max-w-md mx-auto gap-3">
                    <Link
                      href="/tournament/12"
                      className="w-full text-center px-5 py-3 rounded-lg bg-gradient-to-r from-primary to-primary-hover text-on-primary font-semibold text-lg shadow-md hover:shadow-lg hover:scale-105 transform transition"
                    >
                      Voir l'exemple 🔎
                    </Link>
                    <Link
                      href="/frmt/classement/hommes"
                      className="w-full text-center px-5 py-3 rounded-lg bg-background text-on-secondary font-semibold hover:bg-primary-hover transition border border-border"
                    >
                      Classement FRMT
                    </Link>
                    <button
                      onClick={() =>
                        signIn('google', { redirect: true, callbackUrl: '/admin/tournaments' })
                      }
                      className="flex items-center justify-center gap-2 px-5 py-2 text-base bg-card border border-border rounded hover:bg-background transition w-full"
                    >
                      <img src="/google-logo.svg" alt="Google" className="w-5 h-5" />
                      <span className="text-foreground">Se connecter avec Google</span>
                    </button>
                  </div>
                </>
              )}
            </div>

            {/* Features Section */}
            <section>
              <div className="max-w-6xl mx-auto px-4">
                <div className="text-center mb-8">
                  <h2 className="text-3xl sm:text-4xl font-bold text-foreground mb-4">
                    Tout ce dont tu as besoin
                  </h2>
                  <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
                    Des outils puissants et intuitifs pour organiser des tournois mémorables
                  </p>
                </div>

                <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">
                  {features.map((feature, index) => (
                    <article
                      key={index}
                      className="group hover:shadow-card transition-all duration-300 hover:scale-105 hover:-translate-y-2 border border-border bg-card/80 backdrop-blur-sm rounded-lg"
                    >
                      <div className="p-4 text-center">
                        <div>
                          <div className="w-16 h-16 mx-auto bg-gradient-primary rounded-2xl flex items-center justify-center group-hover:animate-pulse transition-all duration-300">
                            <span className="text-3xl">{feature.emoji}</span>
                          </div>
                        </div>

                        <h3 className="text-xl font-bold text-foreground mb-3 group-hover:text-primary transition-colors duration-300">
                          {feature.title}
                        </h3>

                        <p className="text-muted-foreground leading-relaxed">
                          {feature.description}
                        </p>
                      </div>
                    </article>
                  ))}
                </div>
              </div>
            </section>
            <div className="flex justify-center gap-2">
              <ContactButton/>
              <CommunityButton />
            </div>
          </div>
        </div>
      </section>
      {/* Bottom Navigation */}
      <BottomNav items={bottomItems} pathname={pathname} />
    </main>
  );
}

// rm -rf .firebase/functions/.next .next && npm run build:functions && firebase deploy --only functions
// npm run deploy:functions
