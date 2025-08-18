//app/403/page.tsx
export default function ForbiddenPage() {
  return (
    <main className="min-h-screen flex items-center justify-center bg-background p-6">
      <section className="max-w-md w-full bg-card text-card-foreground border border-border rounded-2xl p-7 shadow">
        <div className="text-primary text-6xl font-extrabold mb-2">403</div>
        <h1 className="text-2xl font-bold mb-3 text-foreground">Accès refusé</h1>
        <p className="text-muted-foreground mb-6">
          Vous n'êtes pas autorisé à consulter cette page.
        </p>
        <div className="flex gap-3 flex-wrap">
          <a
            className="px-4 py-2 rounded-lg font-semibold bg-primary text-on-primary border border-primary hover:bg-primary-hover hover:border-primary-hover transition"
            href="/"
          >
            Retour à l’accueil
          </a>
          <a
            className="px-4 py-2 rounded-lg font-semibold border border-border text-foreground hover:bg-muted transition"
            href="/"
          >
            Mes tournois
          </a>
        </div>
      </section>
    </main>
  );
}