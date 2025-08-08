import ConnexionPage from '@/src/components/auth/ConnexionPage';

export default function Home() {
  console.log("page.tsx: NEXTAUTH_SECRET AT BUILD:", process.env.NEXTAUTH_SECRET);
  return <ConnexionPage />;
}

