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
  async rewrites() {
    return [
      // Do NOT proxy NextAuth routes; they must be handled by Next.js
      {
        source: '/api/auth/:path*',
        destination: '/api/auth/:path*',
      },
      // Proxy all other /api requests to the Spring backend
      {
        source: '/api/:path((?!auth).*)', // negative lookahead to exclude auth
        destination: 'http://localhost:8080/:path',
      },
    ];
  },
};

export default nextConfig;