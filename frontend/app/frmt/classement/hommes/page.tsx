import { Metadata } from 'next';
import RankingPageLayout from '@/src/components/frmt-ranking/RankingPageLayout';

export const metadata: Metadata = {
  title: 'Classement National Padel Hommes Maroc',
  description: 'Découvrez le classement officiel des joueurs de padel masculins au Maroc. Suivez les performances, points et évolution des meilleurs joueurs à Casablanca, Rabat, Marrakech, Tanger et dans tout le pays.',
  keywords: 'classement padel homme maroc, padel masculin, joueurs padel hommes, classement national padel, tournois padel maroc, classement casablanca, rabat, marrakech, tanger',
  openGraph: {
    title: 'Classement National Padel Hommes Maroc',
    description: 'Classement officiel des joueurs de padel masculins au Maroc.',
    url: 'https://frmt-revamp-d8c06.web.app/players/men',
  }
};

export default function MenRankingPage() {
  return <RankingPageLayout jsonUrl="ranking-men.json" />;
}