import { NextResponse } from 'next/server';

export function GET() {
  const html = `<!doctype html>
  <html lang="fr">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>403 — Accès refusé</title>
    <style>
      * { box-sizing: border-box; }
      html, body { height: 100%; }
      body {
        margin: 0;
        font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, Cantarell, Noto Sans, Helvetica Neue, Arial, "Apple Color Emoji", "Segoe UI Emoji";
        background: var(--color-background);
        color: var(--color-foreground);
      }
      .wrap { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: var(--color-background); }
      .card {
        max-width: 560px; width: 100%;
        background: var(--color-card);
        color: var(--color-card-foreground);
        border: 1px solid var(--color-border);
        border-radius: 16px; padding: 28px;
      }
      .code { font-size: 56px; line-height: 1; font-weight: 800; letter-spacing: 1px; margin: 0 0 8px; color: var(--color-primary); }
      .title { font-size: 22px; margin: 0 0 10px; font-weight: 700; color: var(--color-foreground); }
      .desc { margin: 0 0 24px; color: var(--color-muted-foreground); }
      .actions { display: flex; gap: 12px; flex-wrap: wrap; }
      .btn { appearance: none; text-decoration: none; display: inline-flex; align-items: center; justify-content: center;
             padding: 10px 14px; border-radius: 10px; font-weight: 600; border: 1px solid transparent; }
      .btn-primary { background: var(--color-primary); color: var(--color-on-primary); border-color: var(--color-primary); }
      .btn-primary:hover { background: var(--color-primary-hover); border-color: var(--color-primary-hover); }
      .btn-ghost { border-color: var(--color-border); color: var(--color-foreground); background: transparent; }
      .small { font-size: 12px; margin-top: 18px; color: var(--color-muted-foreground); }
      a { cursor: pointer; }
    </style>
  </head>
  <body>
    <main class="wrap">
      <section class="card" role="alert" aria-live="polite">
        <div class="code">403</div>
        <h1 class="title">Accès refusé</h1>
        <p class="desc">Vous n'êtes pas autorisé à consulter cette page.</p>
        <div class="actions">
          <a class="btn btn-primary" href="/">Retour à l’accueil</a>
          <a class="btn btn-ghost" href="/">Mes tournois</a>
        </div>
      </section>
    </main>
  </body>
  </html>`;

  return new NextResponse(html, {
    status: 403,
    headers: { 'content-type': 'text/html; charset=utf-8' },
  });
}