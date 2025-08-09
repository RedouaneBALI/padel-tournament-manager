import ConnexionPage from '@/src/components/auth/ConnexionPage';

export default function Home() {
  return <ConnexionPage />;
}

/**
rm -rf .firebase/functions/.next .next && npm run build:functions && firebase deploy --only functions
 */