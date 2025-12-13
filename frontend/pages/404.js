import '../app/globals.css';

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-background text-foreground">
      <h1 className="text-6xl font-bold text-primary mb-4">404</h1>
      <h2 className="text-2xl font-semibold mb-4">Page non trouvée</h2>
      <p className="text-muted-foreground mb-8 text-center">
        Le tournoi que vous cherchez n'existe pas ou a été supprimé.
      </p>
      <a
        href="/"
        className="px-6 py-3 bg-primary text-on-primary rounded hover:bg-primary-hover transition-colors"
      >
        Retour à l'accueil
      </a>
    </div>
  );
}
