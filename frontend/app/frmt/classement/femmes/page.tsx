import { Metadata } from 'next';
import RankingPageLayout from '@/src/components/frmt-ranking/RankingPageLayout';

export const metadata: Metadata = {
  title: 'Classement National Padel Femmes Maroc',
  description: 'Découvrez le classement officiel des joueuses de padel féminines au Maroc. Suivez les performances, points et évolution des meilleures joueuses à Casablanca, Rabat, Marrakech, Tanger et dans tout le pays.',
  keywords: 'classement padel femme maroc, padel féminin, joueuses padel femmes, classement national padel, tournois padel maroc, classement casablanca, rabat, marrakech, tanger',
  openGraph: {
    title: 'Classement National Padel Femmes Maroc',
    description: 'Classement officiel des joueuses de padel féminines au Maroc.',
    url: 'https://www.padelrounds.com/frmt/classement/femmes',
  }
};

export default function WomenRankingPage() {
  return <RankingPageLayout jsonUrl="ranking-women.json" />;
}