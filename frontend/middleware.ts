// frontend/middleware.ts
import { withAuth } from 'next-auth/middleware';

export default withAuth({
  pages: {
    signIn: '/', // redirige vers la page d'accueil si non connecté
  },
});

export const config = {
  matcher: ['/admin/:path*'], // protègera TOUTES les routes commençant par /admin
};