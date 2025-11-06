import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  output: 'standalone', // Important pour Firebase Functions
  // Désactiver l'optimisation d'images pour Firebase
  images: {
    unoptimized: true
  },
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
    ];
  },
};

export default nextConfig;