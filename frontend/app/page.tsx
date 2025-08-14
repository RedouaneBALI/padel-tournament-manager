'use client';

import Link from 'next/link';
import Image from 'next/image';
import { FiPlusCircle, FiList } from 'react-icons/fi';
import { signIn, signOut, useSession } from 'next-auth/react';

export default function Home() {
  const { status } = useSession();

    const features = [
      {
        title: "Création rapide",
        description: "Renseigne quelques infos et démarre en moins d'une minute.",
        emoji: "⚡"
      },
      {
        title: "Paires & tirages",
        description: "Ajoute les équipes, configure les formats, génère le tirage automatiquement.",
        emoji: "👥"
      },
      {
        title: "Gestion en direct",
        description: "Mets à jour les résultats et suis l'avancement du tournoi en temps réel.",
        emoji: "⏱️"
      }
    ];
  // Authenticated: show the main hero with action buttons
  return (
    <main className="min-h-screen bg-background">
      <section className="max-w-5xl mx-auto px-4 py-16 sm:py-24">
        <div className="bg-card border border-border rounded-2xl p-8 sm:p-12 shadow-sm">
          <div className="text-center space-y-6">
            <div className="flex justify-center">
              <Image
                src="/ptm-logo-cropped.png"
                alt="Padel Tournament Manager"
                className="h-40 w-auto"
                width={1300}
                height={1300}
                priority
              />
            </div>

            <p className="text-base sm:text-lg text-muted-foreground max-w-2xl mx-auto">
              Crée, organise et gère tes tournois de padel en quelques clics.
              Une interface simple pour préparer les tableaux, gérer les équipes et suivre les scores.
            </p>

            <div className="flex flex-wrap justify-center gap-3 sm:gap-4">
              {status === 'authenticated' ? (
                <>
                  <Link
                    href="/admin/tournament/new"
                    className="px-5 py-3 rounded-lg bg-primary text-on-primary font-semibold hover:bg-primary-hover transition"
                  >
                    Créer un tournoi
                  </Link>

                  <Link
                    href="/admin/tournaments"
                    className="px-5 py-3 rounded-lg border border-border bg-background text-foreground hover:bg-card transition"
                  >
                    Mes tournois
                  </Link>
                </>
              ) : (
                <div className="flex justify-center">
                  <button
                    onClick={() =>
                      signIn('google', { redirect: true, callbackUrl: '/admin/tournament/new' })
                    }
                    className="flex items-center justify-center gap-2 px-5 py-2 text-base bg-card border border-border rounded hover:bg-background transition"
                  >
                    <img src="/google-logo.svg" alt="Google" className="w-5 h-5" />
                    <span className="text-foreground">Se connecter avec Google</span>
                  </button>
                </div>
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
                          <div
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
                          </div>
                        ))}
                      </div>
                    </div>
                  </section>

          </div>
        </div>
      </section>
    </main>
  );
}

// rm -rf .firebase/functions/.next .next && npm run build:functions && firebase deploy --only functions
// npm run deploy:functions
