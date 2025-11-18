import type { NextConfig } from "next";

// Permettre de configurer dynamiquement les origins autorisés en dev via une variable d'environnement
const allowedDevOriginsEnv = process.env.NEXT_ALLOWED_DEV_ORIGINS;
const allowedDevOrigins = allowedDevOriginsEnv
  ? allowedDevOriginsEnv.split(',').map(s => s.trim()).filter(Boolean)
  : [
      // Par défaut, autorise l'IP locale courante utilisée pour debug mobile (modifie si nécessaire)
      'http://192.168.1.8:3000'
    ];

const nextConfig: NextConfig = {
  reactStrictMode: true,
  output: 'standalone', // Important pour Firebase Functions
  // Désactiver l'optimisation d'images pour Firebase
  images: {
    unoptimized: true
  },
  allowedDevOrigins,
  // Configuration pour éviter les problèmes de build sur Firebase
  trailingSlash: false,
  generateBuildId: async () => {
    return 'build-' + Date.now();
  },
  // Redirections 301 pour SEO - Ancien domaine vers nouveau domaine
  async redirects() {
    return [
      {
        source: '/:path*',
        has: [
          {
            type: 'host',
            value: 'nextapp-tcepdy5iwa-uc.a.run.app',
          },
        ],
        destination: 'https://www.padelrounds.com/:path*',
        permanent: true, // 301 redirect
      },
    ];
  },
  // En-têtes pour SEO et sécurité
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          {
            key: 'X-DNS-Prefetch-Control',
            value: 'on'
          },
          {
            key: 'Strict-Transport-Security',
            value: 'max-age=63072000; includeSubDomains; preload'
          },
          {
            key: 'X-Frame-Options',
            value: 'SAMEORIGIN'
          },
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff'
          },
          {
            key: 'X-XSS-Protection',
            value: '1; mode=block'
          },
          {
            key: 'Referrer-Policy',
            value: 'origin-when-cross-origin'
          },
        ],
      },
      {
        // Empêche l'indexation des pages /admin par les moteurs de recherche
        source: '/admin/:path*',
        headers: [
          {
            key: 'X-Robots-Tag',
            value: 'noindex, nofollow'
          },
        ],
      },
    ];
  },
};

export default nextConfig;